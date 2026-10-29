package com.nebula.stationery.vo;

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
    private Boolean liked;
    private LocalDateTime createdAt;

    /** 追评，没有则为 null */
    private ReviewAppendVO append;
}
