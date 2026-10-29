package com.nebula.cloud.review.feign.dto;

import lombok.Data;

@Data
public class OrderBrief {

    private Long id;

    private Long userId;

    private Integer status;
}
