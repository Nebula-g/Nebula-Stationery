package com.nebula.cloud.order.vo;

import lombok.Data;

/** 服务间只读快照：评价服务用它确认订单归属和状态 */
@Data
public class OrderBrief {

    private Long id;

    private Long userId;

    private Integer status;
}
