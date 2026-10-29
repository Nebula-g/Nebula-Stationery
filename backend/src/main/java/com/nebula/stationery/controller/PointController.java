package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.entity.PointLog;
import com.nebula.stationery.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/my")
    public Result<Map<String, Object>> myPoints(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(pointService.getUserPoints(userId));
    }

    @GetMapping("/log")
    public Result<List<PointLog>> pointLog(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(pointService.getPointLog(userId));
    }

    @PostMapping("/sign-in")
    public Result<Map<String, Object>> signIn(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(pointService.signIn(userId));
    }

    @PostMapping("/redeem")
    public Result<Map<String, Object>> redeem(Authentication authentication, @RequestParam Integer points) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(pointService.redeemPoints(userId, points));
    }

    @GetMapping("/rules")
    public Result<List<Map<String, Object>>> rules() {
        return Result.success(pointService.getRules());
    }
}
