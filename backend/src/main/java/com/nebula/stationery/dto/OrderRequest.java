package com.nebula.stationery.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotEmpty(message = "购物车项不能为空")
    private List<Long> cartItemIds;
}
