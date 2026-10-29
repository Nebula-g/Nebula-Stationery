package com.nebula.cloud.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {

    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    @Size(max = 500, message = "评价内容最多 500 字")
    private String content;

    @Size(max = 6, message = "最多上传 6 张图片")
    private List<String> imageUrls;
}
