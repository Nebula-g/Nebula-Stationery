package com.nebula.cloud.review.feign;

import com.nebula.cloud.common.Result;
import com.nebula.cloud.review.feign.dto.OrderBrief;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/api/internal/orders/{id}")
    Result<OrderBrief> brief(@PathVariable("id") Long id);
}
