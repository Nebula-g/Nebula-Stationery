package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.AppendReviewRequest;
import com.nebula.stationery.dto.ReviewRequest;
import com.nebula.stationery.entity.*;
import com.nebula.stationery.mapper.*;
import com.nebula.stationery.mq.MessageQueueService;
import com.nebula.stationery.mq.handler.ReviewCreatedHandler;
import com.nebula.stationery.vo.ReviewAppendVO;
import com.nebula.stationery.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_IMAGES = 6;

    private final ReviewMapper reviewMapper;
    private final ReviewAppendMapper reviewAppendMapper;
    private final ReviewLikeMapper reviewLikeMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final SensitiveWordService sensitiveWordService;
    private final ObjectMapper objectMapper;
    private final MessageQueueService messageQueueService;

    @Transactional
    public void createReview(Long userId, ReviewRequest request) {
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new BusinessException("只有已完成订单可以评价");
        }

        Review existingReview = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(Review::getProductId, request.getProductId())
                        .eq(Review::getOrderId, request.getOrderId())
        );
        if (existingReview != null) {
            throw new BusinessException("已评价，不能重复评价");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(request.getProductId());
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(sensitiveWordService.filter(request.getContent()));
        review.setImageUrls(toJson(normalizeImages(request.getImageUrls())));
        review.setLikeCount(0);
        reviewMapper.insert(review);

        // 评价落库后发消息，异步清理商品缓存
        messageQueueService.publish(
                MessageQueueService.TOPIC_REVIEW,
                ReviewCreatedHandler.TYPE,
                Map.of("productId", String.valueOf(request.getProductId()))
        );
    }

    /** 追加评价：同一条评价只能追评一次 */
    @Transactional
    public void appendReview(Long userId, AppendReviewRequest request) {
        Review review = reviewMapper.selectById(request.getReviewId());
        if (review == null) {
            throw new BusinessException("原评价不存在");
        }
        if (!review.getUserId().equals(userId)) {
            throw new BusinessException("只能追评自己的评价");
        }

        ReviewAppend existing = reviewAppendMapper.selectOne(
                new LambdaQueryWrapper<ReviewAppend>().eq(ReviewAppend::getReviewId, request.getReviewId())
        );
        if (existing != null) {
            throw new BusinessException("已追评，不能重复追评");
        }

        ReviewAppend append = new ReviewAppend();
        append.setReviewId(request.getReviewId());
        append.setUserId(userId);
        append.setContent(sensitiveWordService.filter(request.getContent()));
        append.setImageUrls(toJson(normalizeImages(request.getImageUrls())));
        reviewAppendMapper.insert(append);
    }

    /**
     * 点赞/取消点赞。
     * @return true 表示当前为已点赞状态
     */
    @Transactional
    public boolean toggleLike(Long userId, Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }

        ReviewLike existing = reviewLikeMapper.selectOne(
                new LambdaQueryWrapper<ReviewLike>()
                        .eq(ReviewLike::getReviewId, reviewId)
                        .eq(ReviewLike::getUserId, userId)
        );

        if (existing != null) {
            reviewLikeMapper.deleteById(existing.getId());
            review.setLikeCount(Math.max(0, review.getLikeCount() - 1));
            reviewMapper.updateById(review);
            return false;
        }

        ReviewLike like = new ReviewLike();
        like.setReviewId(reviewId);
        like.setUserId(userId);
        reviewLikeMapper.insert(like);
        review.setLikeCount(review.getLikeCount() + 1);
        reviewMapper.updateById(review);
        return true;
    }

    public List<ReviewVO> getProductReviews(Long productId, Long currentUserId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .orderByDesc(Review::getCreatedAt)
        );
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> reviewIds = reviews.stream().map(Review::getId).collect(Collectors.toList());

        // 批量取追评，避免 N+1
        Map<Long, ReviewAppend> appendMap = reviewAppendMapper.selectList(
                        new LambdaQueryWrapper<ReviewAppend>().in(ReviewAppend::getReviewId, reviewIds)
                ).stream()
                .collect(Collectors.toMap(ReviewAppend::getReviewId, Function.identity(), (a, b) -> a));

        // 当前用户点赞过的评价
        List<Long> likedReviewIds = currentUserId == null ? Collections.emptyList()
                : reviewLikeMapper.selectList(
                        new LambdaQueryWrapper<ReviewLike>()
                                .in(ReviewLike::getReviewId, reviewIds)
                                .eq(ReviewLike::getUserId, currentUserId)
                ).stream().map(ReviewLike::getReviewId).collect(Collectors.toList());

        // 批量取评价人昵称，避免 N+1
        Map<Long, String> userNames = userMapper.selectBatchIds(
                        reviews.stream().map(Review::getUserId).distinct().collect(Collectors.toList())
                ).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        return reviews.stream().map(review -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(review.getId());
            vo.setUserId(review.getUserId());
            vo.setProductId(review.getProductId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setImageUrls(fromJson(review.getImageUrls()));
            vo.setLikeCount(review.getLikeCount());
            vo.setLiked(likedReviewIds.contains(review.getId()));
            vo.setCreatedAt(review.getCreatedAt());
            vo.setUsername(userNames.get(review.getUserId()));

            ReviewAppend append = appendMap.get(review.getId());
            if (append != null) {
                ReviewAppendVO appendVO = new ReviewAppendVO();
                appendVO.setId(append.getId());
                appendVO.setReviewId(append.getReviewId());
                appendVO.setContent(append.getContent());
                appendVO.setImageUrls(fromJson(append.getImageUrls()));
                appendVO.setCreatedAt(append.getCreatedAt());
                vo.setAppend(appendVO);
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /** 查询某用户在某订单下已提交的评价，用于前端展示"追评"入口 */
    public List<ReviewVO> getUserReviews(Long userId, Long orderId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getUserId, userId)
                        .eq(orderId != null, Review::getOrderId, orderId)
                        .orderByDesc(Review::getCreatedAt)
        );

        return reviews.stream().map(review -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(review.getId());
            vo.setUserId(review.getUserId());
            vo.setProductId(review.getProductId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setImageUrls(fromJson(review.getImageUrls()));
            vo.setLikeCount(review.getLikeCount());
            vo.setCreatedAt(review.getCreatedAt());

            ReviewAppend append = reviewAppendMapper.selectOne(
                    new LambdaQueryWrapper<ReviewAppend>().eq(ReviewAppend::getReviewId, review.getId())
            );
            if (append != null) {
                ReviewAppendVO appendVO = new ReviewAppendVO();
                appendVO.setId(append.getId());
                appendVO.setReviewId(append.getReviewId());
                appendVO.setContent(append.getContent());
                appendVO.setImageUrls(fromJson(append.getImageUrls()));
                appendVO.setCreatedAt(append.getCreatedAt());
                vo.setAppend(appendVO);
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private List<String> normalizeImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return Collections.emptyList();
        }
        if (imageUrls.size() > MAX_IMAGES) {
            throw new BusinessException("最多上传 " + MAX_IMAGES + " 张图片");
        }
        return imageUrls;
    }

    private String toJson(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
