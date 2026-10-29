package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSkuVO {

    private Long id;
    private String skuCode;
    private String specValues;
    private String specText;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private Integer status;
}
