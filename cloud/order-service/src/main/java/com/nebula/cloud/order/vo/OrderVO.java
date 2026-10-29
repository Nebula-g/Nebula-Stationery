package com.nebula.cloud.order.vo;

import com.nebula.cloud.order.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private BigDecimal totalAmount;

    private Integer status;

    private String statusText;

    private LocalDateTime createdAt;

    private List<OrderItem> items;
}
