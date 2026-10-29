package com.nebula.stationery.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductSpecVO {

    private Long id;
    private String specName;
    private List<String> specValues;
    private Integer sortOrder;
}
