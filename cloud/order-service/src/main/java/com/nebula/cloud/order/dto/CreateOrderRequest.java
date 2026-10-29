package com.nebula.cloud.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotEmpty(message = "请至少选择一件商品")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "商品 ID 不能为空")
        private Long productId;

        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量至少为 1")
        private Integer quantity;
    }
}
