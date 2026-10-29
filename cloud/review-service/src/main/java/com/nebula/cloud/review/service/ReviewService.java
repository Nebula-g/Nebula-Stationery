package com.nebula.cloud.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.cloud.common.BusinessException;
import com.nebula.cloud.common.Result;
import com.nebula.cloud.review.dto.ReviewRequest;
import com.nebula.cloud.review.entity.Review;
import com.nebula.cloud.review.feign.OrderClient;
import com.nebula.cloud.review.feign.UserClient;
import com.nebula.cloud.review.feign.dto.OrderBrief;
import com.nebula.cloud.review.mapper.ReviewMapper;
import com.nebula.cloud.review.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderClient orderClient;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public void create(Long userId, ReviewRequest request) {
        // 订单状态在订单服务里，跨服务校验一次
        Result<OrderBrief> orderResult = orderClient.brief(request.getOrderId());
        OrderBrief order = orderResult.getData();
        if (orderResult.getCode() != 200 || order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != 3) {
            throw new BusinessException("只有已完成订单可以评价");
        }

        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getProductId, request.getProductId())
                .eq(Review::getOrderId, request.getOrderId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该订单已评价，不能重复评价");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(request.getContent().trim());
        review.setImageUrls(toJson(request.getImageUrls()));
        review.setLikeCount(0);
        reviewMapper.insert(review);
    }

    public List<ReviewVO> listByProduct(Long productId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .orderByDesc(Review::getId));
        return reviews.stream().map(this::toVO).toList();
    }

    public List<ReviewVO> listMine(Long userId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .orderByDesc(Review::getId));
        return reviews.stream().map(this::toVO).toList();
    }

    private ReviewVO toVO(Review review) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setUserId(review.getUserId());
        vo.setUsername(username(review.getUserId()));
        vo.setProductId(review.getProductId());
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setImageUrls(fromJson(review.getImageUrls()));
        vo.setLikeCount(review.getLikeCount());
        vo.setCreatedAt(review.getCreatedAt());
        return vo;
    }

    private String username(Long userId) {
        try {
            Result<String> result = userClient.username(userId);
            return result.getData() == null ? "匿名用户" : result.getData();
        } catch (Exception e) {
            // 用户服务暂时不可用不该拖垮评价列表
            log.warn("取用户名失败，userId={}", userId);
            return "匿名用户";
        }
    }

    private String toJson(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            throw new BusinessException("图片地址格式不正确");
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
