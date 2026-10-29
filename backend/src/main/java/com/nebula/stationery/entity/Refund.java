package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private Long orderId;

    private Long userId;

    private Integer type;

    private String reason;

    private String description;

    private BigDecimal refundAmount;

    private Integer status;

    private String adminRemark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
