package com.nebula.cloud.review.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {

    private Long id;

    private Long userId;

    private String username;

    private Long productId;

    private Integer rating;

    private String content;

    private List<String> imageUrls;

    private Integer likeCount;

    private LocalDateTime createdAt;
}
