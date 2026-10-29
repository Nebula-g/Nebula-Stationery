package com.nebula.stationery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nebula.stationery.common.Result;
import com.nebula.stationery.service.OrderService;
import com.nebula.stationery.service.RefundService;
import com.nebula.stationery.vo.OrderVO;
import com.nebula.stationery.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final RefundService refundService;

    @GetMapping("/orders")
    public Result<Page<OrderVO>> getOrderList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<OrderVO> result = orderService.getAllOrders(status, page, size);
        return Result.success(result);
    }

    @GetMapping("/orders/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        OrderVO result = orderService.getAdminOrderDetail(id);
        return Result.success(result);
    }

    @PutMapping("/orders/{id}/ship")
    public Result<?> shipOrder(@PathVariable Long id) {
        orderService.shipOrder(id);
        return Result.success("发货成功", null);
    }

    @GetMapping("/refunds")
    public Result<Page<RefundVO>> getRefundList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<RefundVO> result = refundService.getAllRefunds(status, page, size);
        return Result.success(result);
    }

    @PutMapping("/refunds/{id}/approve")
    public Result<?> approveRefund(@PathVariable Long id) {
        refundService.approveRefund(id);
        return Result.success("审批通过", null);
    }

    @PutMapping("/refunds/{id}/reject")
    public Result<?> rejectRefund(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String remark = body.getOrDefault("remark", "");
        refundService.rejectRefund(id, remark);
        return Result.success("已拒绝", null);
    }
}
