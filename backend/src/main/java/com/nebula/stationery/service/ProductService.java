package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.stationery.cache.CacheService;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.ProductRequest;
import com.nebula.stationery.entity.Category;
import com.nebula.stationery.entity.Product;
import com.nebula.stationery.entity.ProductSku;
import com.nebula.stationery.entity.ProductSpec;
import com.nebula.stationery.mapper.CategoryMapper;
import com.nebula.stationery.mapper.ProductMapper;
import com.nebula.stationery.mapper.ProductSkuMapper;
import com.nebula.stationery.mapper.ProductSpecMapper;
import com.nebula.stationery.vo.CategoryVO;
import com.nebula.stationery.vo.ProductSkuVO;
import com.nebula.stationery.vo.ProductSpecVO;
import com.nebula.stationery.vo.ProductSuggestVO;
import com.nebula.stationery.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String LIST_PREFIX = "stationery:product:list:";
    private static final String DETAIL_PREFIX = "stationery:product:detail:";
    private static final String SUGGEST_PREFIX = "stationery:product:suggest:";
    private static final String CATEGORY_TREE_KEY = "stationery:category:tree";
    private static final String HOT_KEYWORD_PREFIX = "stationery:product:hot:";

    private static final Duration LIST_TTL = Duration.ofSeconds(120);
    private static final Duration DETAIL_TTL = Duration.ofSeconds(300);
    private static final Duration SUGGEST_TTL = Duration.ofSeconds(60);
    private static final Duration CATEGORY_TTL = Duration.ofHours(6);

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ObjectMapper objectMapper;
    private final CacheService cacheService;

    /**
     * 商品列表按「分类+关键词+分页」缓存 2 分钟：
     * 一次列表查询要为每个商品补分类名、规格和 SKU，是全站最贵的读请求。
     */
    public Page<ProductVO> getProductList(Long categoryId, String keyword, Integer page, Integer size) {
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        String key = LIST_PREFIX
                + (categoryId == null ? "all" : categoryId) + ":"
                + (trimmed == null ? "-" : trimmed) + ":" + page + ":" + size;
        return cacheService.getOrLoad(
                key,
                new TypeReference<Page<ProductVO>>() {},
                LIST_TTL,
                () -> loadProductList(categoryId, trimmed, page, size)
        );
    }

    private Page<ProductVO> loadProductList(Long categoryId, String keyword, Integer page, Integer size) {
        Page<Product> productPage = new Page<>(page, size);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1); // 只查询上架商品
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Product::getName, kw).or().like(Product::getDescription, kw));
        }
        if (categoryId != null) {
            // 检查是否为一级分类，如果是则查询其下所有二级分类的商品
            Category category = categoryMapper.selectById(categoryId);
            if (category != null && category.getParentId() == null) {
                // 一级分类，查找所有子分类
                List<Category> children = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>().eq(Category::getParentId, categoryId)
                );
                List<Long> childIds = children.stream().map(Category::getId).collect(Collectors.toList());
                if (!childIds.isEmpty()) {
                    wrapper.in(Product::getCategoryId, childIds);
                } else {
                    wrapper.eq(Product::getCategoryId, categoryId);
                }
            } else {
                // 二级分类，直接查询
                wrapper.eq(Product::getCategoryId, categoryId);
            }
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        productMapper.selectPage(productPage, wrapper);

        // 转换为 VO
        Page<ProductVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<ProductVO> voList = productPage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);

        return voPage;
    }

    public ProductVO getProductDetail(Long id) {
        return cacheService.getOrLoad(
                DETAIL_PREFIX + id,
                new TypeReference<ProductVO>() {},
                DETAIL_TTL,
                () -> loadProductDetail(id)
        );
    }

    private ProductVO loadProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在");
        }
        return convertToVO(product);
    }

    public List<CategoryVO> getCategoryList() {
        return cacheService.getOrLoad(
                CATEGORY_TREE_KEY,
                new TypeReference<List<CategoryVO>>() {},
                CATEGORY_TTL,
                () -> {
                    List<Category> categories = categoryMapper.selectList(
                            new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder)
                    );
                    // 构建树形结构
                    return buildCategoryTree(categories, null);
                }
        );
    }

    /**
     * 搜索联想：按关键词匹配商品名，销量高者优先，最多返回 10 条。
     */
    public List<ProductSuggestVO> suggest(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String kw = keyword.trim();
        return cacheService.getOrLoad(
                SUGGEST_PREFIX + kw,
                new TypeReference<List<ProductSuggestVO>>() {},
                SUGGEST_TTL,
                () -> loadSuggest(kw)
        );
    }

    private List<ProductSuggestVO> loadSuggest(String kw) {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .like(Product::getName, kw)
                        .orderByDesc(Product::getSoldCount)
                        .orderByAsc(Product::getId)
                        .last("LIMIT 10")
        );

        return products.stream().map(p -> {
            ProductSuggestVO vo = new ProductSuggestVO();
            vo.setId(p.getId());
            vo.setName(p.getName());
            vo.setPrice(p.getPrice());
            vo.setImageUrl(p.getImageUrl());
            vo.setSoldCount(p.getSoldCount());
            return vo;
        }).collect(Collectors.toList());
    }

    /** 热门搜索词：按销量取前 N 个商品名，搜索框为空时展示 */
    public List<String> hotKeywords(int limit) {
        int size = Math.max(1, Math.min(limit, 20));
        return cacheService.getOrLoad(
                HOT_KEYWORD_PREFIX + size,
                new TypeReference<List<String>>() {},
                SUGGEST_TTL,
                () -> loadHotKeywords(size)
        );
    }

    private List<String> loadHotKeywords(int size) {
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSoldCount)
                        .last("LIMIT " + size)
        );
        return products.stream().map(Product::getName).distinct().collect(Collectors.toList());
    }

    @Transactional
    public Long createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(1); // 默认上架
        productMapper.insert(product);
        evictProductCache(product.getId());
        return product.getId();
    }

    @Transactional
    public void updateProduct(Long id, ProductRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategoryId(request.getCategoryId());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        productMapper.updateById(product);
        evictProductCache(id);
    }

    @Transactional
    public void updateProductStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        evictProductCache(id);
    }

    /** 商品写操作后清掉详情、列表、联想和热门词，避免前台看到旧价格和旧库存 */
    public void evictProductCache(Long productId) {
        cacheService.evict(DETAIL_PREFIX + productId);
        cacheService.evictPrefix(LIST_PREFIX);
        cacheService.evictPrefix(SUGGEST_PREFIX);
        cacheService.evictPrefix(HOT_KEYWORD_PREFIX);
    }

    private ProductVO convertToVO(Product product) {
        ProductVO vo = new ProductVO();
        vo.setId(product.getId());
        vo.setName(product.getName());
        vo.setPrice(product.getPrice());
        vo.setStock(product.getStock());
        vo.setCategoryId(product.getCategoryId());
        vo.setDescription(product.getDescription());
        vo.setImageUrl(product.getImageUrl());
        vo.setStatus(product.getStatus());
        vo.setCreatedAt(product.getCreatedAt());

        Category category = categoryMapper.selectById(product.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }

        List<ProductSpec> specs = productSpecMapper.selectList(
                new LambdaQueryWrapper<ProductSpec>()
                        .eq(ProductSpec::getProductId, product.getId())
                        .orderByAsc(ProductSpec::getSortOrder)
        );
        if (!specs.isEmpty()) {
            vo.setHasSpec(true);
            vo.setSpecs(specs.stream().map(spec -> {
                ProductSpecVO specVO = new ProductSpecVO();
                specVO.setId(spec.getId());
                specVO.setSpecName(spec.getSpecName());
                specVO.setSortOrder(spec.getSortOrder());
                try {
                    specVO.setSpecValues(objectMapper.readValue(spec.getSpecValues(), new TypeReference<List<String>>() {}));
                } catch (Exception e) {
                    specVO.setSpecValues(Collections.emptyList());
                }
                return specVO;
            }).collect(Collectors.toList()));
        } else {
            vo.setHasSpec(false);
        }

        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, product.getId())
                        .eq(ProductSku::getStatus, 1)
        );
        if (!skus.isEmpty()) {
            vo.setSkus(skus.stream().map(sku -> {
                ProductSkuVO skuVO = new ProductSkuVO();
                skuVO.setId(sku.getId());
                skuVO.setSkuCode(sku.getSkuCode());
                skuVO.setSpecValues(sku.getSpecValues());
                skuVO.setSpecText(sku.getSpecText());
                skuVO.setPrice(sku.getPrice());
                skuVO.setStock(sku.getStock());
                skuVO.setImageUrl(sku.getImageUrl());
                skuVO.setStatus(sku.getStatus());
                return skuVO;
            }).collect(Collectors.toList()));
        }

        return vo;
    }

    private List<CategoryVO> buildCategoryTree(List<Category> categories, Long parentId) {
        return categories.stream()
                .filter(c -> (c.getParentId() == null && parentId == null) ||
                             (c.getParentId() != null && c.getParentId().equals(parentId)))
                .map(c -> {
                    CategoryVO vo = new CategoryVO();
                    vo.setId(c.getId());
                    vo.setName(c.getName());
                    vo.setParentId(c.getParentId());
                    vo.setSortOrder(c.getSortOrder());
                    vo.setImageUrl(c.getImageUrl());
                    vo.setChildren(buildCategoryTree(categories, c.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
