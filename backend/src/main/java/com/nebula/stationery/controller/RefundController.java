package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.RefundRequest;
import com.nebula.stationery.service.RefundService;
import com.nebula.stationery.vo.RefundVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public Result<Long> applyRefund(Authentication authentication, @Valid @RequestBody RefundRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        Long id = refundService.applyRefund(userId, request);
        return Result.success("申请成功", id);
    }

    @GetMapping
    public Result<List<RefundVO>> getUserRefunds(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<RefundVO> result = refundService.getUserRefunds(userId);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<RefundVO> getRefundDetail(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        RefundVO result = refundService.getRefundDetail(userId, id);
        return Result.success(result);
    }

    @PutMapping("/{id}/cancel")
    public Result<?> cancelRefund(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        refundService.cancelRefund(userId, id);
        return Result.success("取消成功", null);
    }
}
