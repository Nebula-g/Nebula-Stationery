package com.nebula.stationery.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewAppendVO {

    private Long id;
    private Long reviewId;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
