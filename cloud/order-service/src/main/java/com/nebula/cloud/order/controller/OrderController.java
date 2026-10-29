package com.nebula.cloud.order.controller;

import com.nebula.cloud.common.CurrentUser;
import com.nebula.cloud.common.PageResult;
import com.nebula.cloud.common.Result;
import com.nebula.cloud.order.dto.CreateOrderRequest;
import com.nebula.cloud.order.service.OrderService;
import com.nebula.cloud.order.vo.OrderVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> create(HttpServletRequest request, @Valid @RequestBody CreateOrderRequest body) {
        return Result.success("下单成功", orderService.create(CurrentUser.id(request), body));
    }

    @GetMapping
    public Result<PageResult<OrderVO>> list(HttpServletRequest request,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(orderService.page(CurrentUser.id(request), page, size, status));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.detail(CurrentUser.id(request), id));
    }

    @PutMapping("/{id}/pay")
    public Result<Void> pay(HttpServletRequest request, @PathVariable Long id) {
        orderService.pay(CurrentUser.id(request), id);
        return Result.success("支付成功", null);
    }

    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(HttpServletRequest request, @PathVariable Long id) {
        orderService.cancel(CurrentUser.id(request), id);
        return Result.success("订单已取消", null);
    }

    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(HttpServletRequest request, @PathVariable Long id) {
        orderService.confirm(CurrentUser.id(request), id);
        return Result.success("已确认收货", null);
    }
}
