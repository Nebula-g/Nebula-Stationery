package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics")
public class Logistics {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String companyCode;

    private String companyName;

    private String trackingNo;

    private String receiverPhone;

    private Integer status;

    private LocalDateTime shippedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
