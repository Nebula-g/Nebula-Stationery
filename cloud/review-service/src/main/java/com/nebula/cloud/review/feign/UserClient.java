package com.nebula.cloud.review.feign;

import com.nebula.cloud.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/api/internal/users/{id}/username")
    Result<String> username(@PathVariable("id") Long id);
}
