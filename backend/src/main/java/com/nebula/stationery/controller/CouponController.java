package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.entity.Coupon;
import com.nebula.stationery.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public Result<List<Coupon>> listCoupons() {
        return Result.success(couponService.listAvailableCoupons());
    }

    @PostMapping("/claim/{couponId}")
    public Result<?> claimCoupon(Authentication authentication, @PathVariable Long couponId) {
        Long userId = (Long) authentication.getPrincipal();
        couponService.claimCoupon(userId, couponId);
        return Result.success("领取成功", null);
    }

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myCoupons(Authentication authentication,
                                                        @RequestParam(required = false) Integer status) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(couponService.getUserCoupons(userId, status));
    }

    @PostMapping("/apply")
    public Result<Map<String, Object>> applyCoupon(Authentication authentication,
                                                    @RequestParam Long userCouponId,
                                                    @RequestParam BigDecimal orderAmount) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(couponService.applyCoupon(userId, userCouponId, orderAmount));
    }
}
