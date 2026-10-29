package com.nebula.cloud.review.controller;

import com.nebula.cloud.common.CurrentUser;
import com.nebula.cloud.common.Result;
import com.nebula.cloud.review.dto.ReviewRequest;
import com.nebula.cloud.review.service.ReviewService;
import com.nebula.cloud.review.vo.ReviewVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Result<Void> create(HttpServletRequest request, @Valid @RequestBody ReviewRequest body) {
        reviewService.create(CurrentUser.id(request), body);
        return Result.success("评价成功", null);
    }

    @GetMapping("/product/{productId}")
    public Result<List<ReviewVO>> byProduct(@PathVariable Long productId) {
        return Result.success(reviewService.listByProduct(productId));
    }

    @GetMapping("/my")
    public Result<List<ReviewVO>> mine(HttpServletRequest request) {
        return Result.success(reviewService.listMine(CurrentUser.id(request)));
    }
}
