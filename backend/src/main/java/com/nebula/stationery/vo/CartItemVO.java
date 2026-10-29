package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemVO {

    private Long id;
    private Long productId;
    private Long skuId;
    private String specText;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;
    private Integer status;
}
