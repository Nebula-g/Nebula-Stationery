package com.nebula.cloud.order.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockChange {

    private Long productId;

    private Integer quantity;
}
