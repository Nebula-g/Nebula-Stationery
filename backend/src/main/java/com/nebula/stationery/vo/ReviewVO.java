package com.nebula.stationery.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {

    private Long id;
    private Long userId;
    private String username;
    private Long productId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
