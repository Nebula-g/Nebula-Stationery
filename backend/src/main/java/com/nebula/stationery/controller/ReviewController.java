package com.nebula.stationery.controller;

import com.nebula.stationery.common.Result;
import com.nebula.stationery.dto.AppendReviewRequest;
import com.nebula.stationery.dto.ReviewRequest;
import com.nebula.stationery.service.ReviewService;
import com.nebula.stationery.service.SensitiveWordService;
import com.nebula.stationery.vo.ReviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final SensitiveWordService sensitiveWordService;

    @PostMapping
    public Result<?> createReview(Authentication authentication, @Valid @RequestBody ReviewRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        reviewService.createReview(userId, request);
        return Result.success("评价成功", null);
    }

    @PostMapping("/append")
    public Result<?> appendReview(Authentication authentication, @Valid @RequestBody AppendReviewRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        reviewService.appendReview(userId, request);
        return Result.success("追评成功", null);
    }

    @PutMapping("/{id}/like")
    public Result<Map<String, Object>> toggleLike(Authentication authentication, @PathVariable Long id) {
        Long userId = (Long) authentication.getPrincipal();
        boolean liked = reviewService.toggleLike(userId, id);
        return Result.success(Map.of("liked", liked));
    }

    @GetMapping("/product/{productId}")
    public Result<List<ReviewVO>> getProductReviews(
            @PathVariable Long productId,
            Authentication authentication) {
        // 匿名访问时 principal 是 "anonymousUser"，只有 JWT 认证的才是 Long
        Long currentUserId = authentication != null && authentication.getPrincipal() instanceof Long id ? id : null;
        return Result.success(reviewService.getProductReviews(productId, currentUserId));
    }

    @GetMapping("/mine")
    public Result<List<ReviewVO>> getMyReviews(
            Authentication authentication,
            @RequestParam(required = false) Long orderId) {
        Long userId = (Long) authentication.getPrincipal();
        return Result.success(reviewService.getUserReviews(userId, orderId));
    }

    /** 前端提交前的敏感词预检，仅返回是否命中，不返回词库内容 */
    @PostMapping("/sensitive-check")
    public Result<Map<String, Object>> checkSensitive(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        boolean hit = sensitiveWordService.containsSensitiveWord(text);
        return Result.success(Map.of(
                "sensitive", hit,
                "filtered", sensitiveWordService.filter(text)
        ));
    }
}
