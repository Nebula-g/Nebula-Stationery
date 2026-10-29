package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String imageUrl;
    private Integer status;
    private LocalDateTime createdAt;
    private List<ProductSpecVO> specs;
    private List<ProductSkuVO> skus;
    private Boolean hasSpec;
}
