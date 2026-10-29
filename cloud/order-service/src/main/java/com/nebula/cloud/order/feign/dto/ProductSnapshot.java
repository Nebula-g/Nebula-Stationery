package com.nebula.cloud.order.feign.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSnapshot {

    private Long id;

    private String name;

    private BigDecimal price;

    private Integer stock;

    private Integer status;
}
