package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSuggestVO {

    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Integer soldCount;
}
