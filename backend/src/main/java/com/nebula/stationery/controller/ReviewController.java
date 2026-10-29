package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.ReviewRequest;
import com.nebula.stationery.service.ReviewService;
import com.nebula.stationery.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Result<?> createReview(Authentication authentication, @Valid @RequestBody ReviewRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        reviewService.createReview(userId, request);
        return Result.success("评价成功", null);
    }

    @GetMapping("/product/{productId}")
    public Result<List<ReviewVO>> getProductReviews(@PathVariable Long productId) {
        List<ReviewVO> result = reviewService.getProductReviews(productId);
        return Result.success(result);
    }
}
