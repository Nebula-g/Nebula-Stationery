package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CategorySalesVO {

    private String name;

    private Integer soldCount;

    private BigDecimal amount;
}
