package com.nebula.stationery.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatOverviewVO {

    private Integer totalOrderCount;

    private BigDecimal totalRevenue;

    private Integer todayOrderCount;

    private BigDecimal todayRevenue;

    private Integer totalUserCount;

    private Integer onSaleProductCount;

    private Integer lowStockCount;

    private Integer pendingShipCount;
}
