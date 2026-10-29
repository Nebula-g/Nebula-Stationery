package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product_sku")
public class ProductSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String skuCode;

    private String specValues;

    private String specText;

    private BigDecimal price;

    private Integer stock;

    private String imageUrl;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
