package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("point_rule")
public class PointRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleKey;

    private Integer ruleValue;

    private String description;
}
