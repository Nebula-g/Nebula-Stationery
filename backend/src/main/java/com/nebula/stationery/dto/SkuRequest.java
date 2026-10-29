package com.nebula.stationery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuRequest {

    private String skuCode;
    private String specValues;
    private String specText;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private Integer status;
}
