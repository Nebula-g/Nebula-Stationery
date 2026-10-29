package com.nebula.stationery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.stationery.common.Result;
import com.nebula.stationery.service.ProductService;
import com.nebula.stationery.vo.CategoryVO;
import com.nebula.stationery.vo.ProductSuggestVO;
import com.nebula.stationery.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public Result<Page<ProductVO>> getProductList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<ProductVO> result = productService.getProductList(categoryId, keyword, page, size);
        return Result.success(result);
    }

    @GetMapping("/products/suggest")
    public Result<List<ProductSuggestVO>> suggest(@RequestParam(required = false) String keyword) {
        return Result.success(productService.suggest(keyword));
    }

    @GetMapping("/products/hot-keywords")
    public Result<List<String>> hotKeywords(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.success(productService.hotKeywords(limit));
    }

    @GetMapping("/products/{id}")
    public Result<ProductVO> getProductDetail(@PathVariable Long id) {
        ProductVO result = productService.getProductDetail(id);
        return Result.success(result);
    }

    @GetMapping("/categories")
    public Result<List<CategoryVO>> getCategoryList() {
        List<CategoryVO> result = productService.getCategoryList();
        return Result.success(result);
    }
}
