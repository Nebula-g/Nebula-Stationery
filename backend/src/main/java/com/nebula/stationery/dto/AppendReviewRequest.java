package com.nebula.stationery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AppendReviewRequest {

    @NotNull(message = "评价 ID 不能为空")
    private Long reviewId;

    @NotBlank(message = "追评内容不能为空")
    private String content;

    private List<String> imageUrls;
}
