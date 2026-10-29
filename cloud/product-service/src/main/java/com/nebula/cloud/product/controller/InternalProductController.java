package com.nebula.cloud.product.controller;

import com.nebula.cloud.common.Result;
import com.nebula.cloud.product.dto.StockChange;
import com.nebula.cloud.product.entity.Product;
import com.nebula.cloud.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 服务间接口：订单服务通过 Feign 调用，网关不对外路由 */
@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.raw(id));
    }

    @PostMapping("/deduct")
    public Result<Void> deduct(@Valid @RequestBody List<StockChange> changes) {
        productService.deduct(changes);
        return Result.success("库存扣减成功", null);
    }

    @PostMapping("/restore")
    public Result<Void> restore(@Valid @RequestBody List<StockChange> changes) {
        productService.restore(changes);
        return Result.success("库存已回滚", null);
    }
}
