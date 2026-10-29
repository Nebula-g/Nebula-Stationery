package com.nebula.cloud.product.controller;

import com.nebula.cloud.common.Result;
import com.nebula.cloud.product.entity.Category;
import com.nebula.cloud.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductService productService;

    @GetMapping
    public Result<List<Category>> list() {
        return Result.success(productService.categories());
    }
}
