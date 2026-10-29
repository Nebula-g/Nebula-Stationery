package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundVO {

    private Long id;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private Integer type;
    private String typeText;
    private String reason;
    private String description;
    private BigDecimal refundAmount;
    private Integer status;
    private String statusText;
    private String adminRemark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
