package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("flash_sale_order")
public class FlashSaleOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long flashSaleId;

    private Long productId;

    private Integer quantity;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime createdAt;
}
