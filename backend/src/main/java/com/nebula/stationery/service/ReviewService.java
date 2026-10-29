package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.ReviewRequest;
import com.nebula.stationery.entity.Order;
import com.nebula.stationery.entity.Review;
import com.nebula.stationery.entity.User;
import com.nebula.stationery.mapper.OrderMapper;
import com.nebula.stationery.mapper.ReviewMapper;
import com.nebula.stationery.mapper.UserMapper;
import com.nebula.stationery.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;

    @Transactional
    public void createReview(Long userId, ReviewRequest request) {
        // 检查订单是否存在且属于该用户
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }

        // 检查订单状态是否为已完成
        if (order.getStatus() != 3) {
            throw new BusinessException("只有已完成订单可以评价");
        }

        // 检查是否已评价
        Review existingReview = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(Review::getProductId, request.getProductId())
                        .eq(Review::getOrderId, request.getOrderId())
        );
        if (existingReview != null) {
            throw new BusinessException("已评价，不能重复评价");
        }

        // 创建评价
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        reviewMapper.insert(review);
    }

    public List<ReviewVO> getProductReviews(Long productId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .orderByDesc(Review::getCreatedAt)
        );

        return reviews.stream().map(review -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(review.getId());
            vo.setUserId(review.getUserId());
            vo.setProductId(review.getProductId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setCreatedAt(review.getCreatedAt());

            User user = userMapper.selectById(review.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
