package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("flash_sale")
public class FlashSale {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private BigDecimal flashPrice;

    private Integer flashStock;

    private Integer soldCount;

    private Integer perLimit;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private LocalDateTime createdAt;
}
