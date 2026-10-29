package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSalesVO {

    private Long productId;

    private String name;

    private Integer soldCount;

    private BigDecimal amount;
}
