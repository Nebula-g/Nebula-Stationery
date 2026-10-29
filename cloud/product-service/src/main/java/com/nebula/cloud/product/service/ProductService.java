package com.nebula.cloud.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.cloud.common.BusinessException;
import com.nebula.cloud.common.PageResult;
import com.nebula.cloud.product.dto.StockChange;
import com.nebula.cloud.product.entity.Category;
import com.nebula.cloud.product.entity.Product;
import com.nebula.cloud.product.mapper.CategoryMapper;
import com.nebula.cloud.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    public PageResult<Product> page(int page, int size, Long categoryId, String keyword) {
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSoldCount)
                .orderByDesc(Product::getId);
        if (categoryId != null) {
            query.eq(Product::getCategoryId, categoryId);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(Product::getName, keyword.trim());
        }

        Page<Product> result = productMapper.selectPage(new Page<>(page, size), query);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    public Product detail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == null || product.getStatus() != 1) {
            throw new BusinessException(404, "商品不存在或已下架");
        }
        return product;
    }

    /** 服务间调用：下单时要拿到价格和库存，未上架的商品也应该能查到 */
    public Product raw(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(404, "商品不存在");
        }
        return product;
    }

    public List<Category> categories() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder).orderByAsc(Category::getId));
    }

    @Transactional
    public void deduct(List<StockChange> changes) {
        for (StockChange change : changes) {
            if (productMapper.deductStock(change.getProductId(), change.getQuantity()) == 0) {
                throw new BusinessException("商品「" + name(change.getProductId()) + "」库存不足");
            }
        }
    }

    @Transactional
    public void restore(List<StockChange> changes) {
        for (StockChange change : changes) {
            productMapper.restoreStock(change.getProductId(), change.getQuantity());
        }
    }

    private String name(Long productId) {
        Product product = productMapper.selectById(productId);
        return product == null ? String.valueOf(productId) : product.getName();
    }
}
