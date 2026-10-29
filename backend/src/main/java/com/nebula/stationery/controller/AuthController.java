package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.LoginRequest;
import com.nebula.stationery.dto.RegisterRequest;
import com.nebula.stationery.dto.SmsLoginRequest;
import com.nebula.stationery.dto.SmsSendRequest;
import com.nebula.stationery.service.AuthService;
import com.nebula.stationery.service.SmsCodeService;
import com.nebula.stationery.vo.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsCodeService smsCodeService;

    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    @PostMapping("/sms/send")
    public Result<Map<String, Object>> sendSmsCode(@Valid @RequestBody SmsSendRequest request) {
        return Result.success("验证码已发送", smsCodeService.send(request.getPhone()));
    }

    @PostMapping("/sms/login")
    public Result<LoginResponse> loginBySms(@Valid @RequestBody SmsLoginRequest request) {
        return Result.success("登录成功", authService.loginByPhone(request.getPhone(), request.getCode()));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success("登出成功", null);
    }
}
