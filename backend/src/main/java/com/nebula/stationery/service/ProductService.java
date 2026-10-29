package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.nebula.stationery.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductSpecMapper productSpecMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ObjectMapper objectMapper;

    public Page<ProductVO> getProductList(Long categoryId, Integer page, Integer size) {
        Page<Product> productPage = new Page<>(page, size);

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1); // 只查询上架商品
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
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在");
        }
        return convertToVO(product);
    }

    public List<CategoryVO> getCategoryList() {
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder)
        );

        // 构建树形结构
        return buildCategoryTree(categories, null);
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
    }

    @Transactional
    public void updateProductStatus(Long id, Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
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
