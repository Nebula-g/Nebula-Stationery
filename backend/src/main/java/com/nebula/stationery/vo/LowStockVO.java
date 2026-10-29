package com.nebula.stationery.vo;

import lombok.Data;

@Data
public class LowStockVO {

    private Long id;

    private String name;

    private String categoryName;

    private Integer stock;

    private Integer soldCount;
}
