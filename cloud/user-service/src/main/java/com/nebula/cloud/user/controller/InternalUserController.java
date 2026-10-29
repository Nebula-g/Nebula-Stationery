package com.nebula.cloud.user.controller;

import com.nebula.cloud.common.Result;
import com.nebula.cloud.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务间调用入口：网关不对外暴露 /api/internal/**，只供其他微服务用 Feign 访问。
 */
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}/username")
    public Result<String> username(@PathVariable Long id) {
        return Result.success(userService.username(id));
    }
}
