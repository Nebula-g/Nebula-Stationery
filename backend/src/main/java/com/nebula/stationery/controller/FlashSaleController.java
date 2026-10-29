package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.entity.FlashSaleOrder;
import com.nebula.stationery.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping
    public Result<List<Map<String, Object>>> listFlashSales() {
        return Result.success(flashSaleService.listFlashSales());
    }

    @PostMapping("/participate")
    public Result<Map<String, Object>> participate(Authentication authentication,
                                                    @RequestParam Long flashSaleId,
                                                    @RequestParam(defaultValue = "1") Integer quantity) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(flashSaleService.participate(userId, flashSaleId, quantity));
    }

    @GetMapping("/my-orders")
    public Result<List<FlashSaleOrder>> myOrders(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(flashSaleService.getUserFlashSaleOrders(userId));
    }

    @PostMapping("/pay/{orderId}")
    public Result<?> payOrder(Authentication authentication, @PathVariable Long orderId) {
        Long userId = (Long) authentication.getPrincipal();
        flashSaleService.payFlashSaleOrder(userId, orderId);
        return Result.success("支付成功", null);
    }

    @PostMapping("/cancel/{orderId}")
    public Result<?> cancelOrder(Authentication authentication, @PathVariable Long orderId) {
        Long userId = (Long) authentication.getPrincipal();
        flashSaleService.cancelFlashSaleOrder(userId, orderId);
        return Result.success("取消成功", null);
    }
}
