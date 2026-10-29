package com.nebula.cloud.product.controller;

import com.nebula.cloud.common.PageResult;
import com.nebula.cloud.common.Result;
import com.nebula.cloud.product.entity.Product;
import com.nebula.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public Result<PageResult<Product>> list(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "12") int size,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) String keyword) {
        return Result.success(productService.page(page, size, categoryId, keyword));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }
}
