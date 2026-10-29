package com.nebula.cloud.order.controller;

import com.nebula.cloud.common.Result;
import com.nebula.cloud.order.service.OrderService;
import com.nebula.cloud.order.vo.OrderBrief;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 服务间接口：评价服务校验订单归属和完成状态 */
@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public Result<OrderBrief> brief(@PathVariable Long id) {
        return Result.success(orderService.brief(id));
    }
}
