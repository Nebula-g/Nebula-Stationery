package com.nebula.cloud.order.feign;

import com.nebula.cloud.common.Result;
import com.nebula.cloud.order.feign.dto.ProductSnapshot;
import com.nebula.cloud.order.feign.dto.StockChange;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 通过服务名调用商品服务，Eureka + LoadBalancer 负责找到实例，
 * 所以这里完全不出现 IP 和端口。
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/internal/products/{id}")
    Result<ProductSnapshot> detail(@PathVariable("id") Long id);

    @PostMapping("/api/internal/products/deduct")
    Result<Void> deduct(@RequestBody List<StockChange> changes);

    @PostMapping("/api/internal/products/restore")
    Result<Void> restore(@RequestBody List<StockChange> changes);
}
