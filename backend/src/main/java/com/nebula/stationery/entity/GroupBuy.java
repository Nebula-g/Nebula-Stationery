package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("group_buy")
public class GroupBuy {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private BigDecimal groupPrice;

    private Integer minGroupSize;

    private Integer maxGroupSize;

    private Integer totalGroups;

    private LocalDateTime createdAt;
}
