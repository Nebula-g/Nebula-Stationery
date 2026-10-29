package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private String orderType;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private LocalDateTime createdAt;
    private List<OrderItemVO> items;
    private LogisticsVO logistics;
}
