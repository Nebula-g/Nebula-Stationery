package com.nebula.stationery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartRequest {

    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    private Long skuId;

    @NotNull(message = "数量不能为空")
    private Integer quantity;
}
