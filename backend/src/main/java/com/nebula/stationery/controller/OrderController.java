package com.nebula.stationery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.OrderRequest;
import com.nebula.stationery.service.OrderService;
import com.nebula.stationery.vo.LogisticsVO;
import com.nebula.stationery.vo.OrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> createOrder(Authentication authentication, @Valid @RequestBody OrderRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        OrderVO result = orderService.createOrder(userId, request);
        return Result.success("下单成功", result);
    }

    @GetMapping
    public Result<Page<OrderVO>> getOrderList(Authentication authentication,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        Long userId = (Long) authentication.getPrincipal();
        Page<OrderVO> result = orderService.getOrderList(userId, status, page, size);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        OrderVO result = orderService.getOrderDetail(id, userId);
        return Result.success(result);
    }

    /** 单独拉取物流轨迹，订单详情页点「刷新物流」时用，避免整页重载 */
    @GetMapping("/{id}/logistics")
    public Result<LogisticsVO> getLogistics(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        OrderVO order = orderService.getOrderDetail(id, userId);
        return Result.success(order.getLogistics());
    }

    @PutMapping("/{id}/pay")
    public Result<?> payOrder(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        orderService.payOrder(userId, id);
        return Result.success("付款成功", null);
    }

    @PutMapping("/{id}/cancel")
    public Result<?> cancelOrder(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        orderService.cancelOrder(userId, id);
        return Result.success("取消成功", null);
    }

    @PutMapping("/{id}/confirm")
    public Result<?> confirmOrder(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        orderService.confirmOrder(userId, id);
        return Result.success("确认收货成功", null);
    }
}
