package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("point_log")
public class PointLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer points;

    private Integer type;

    private Long relatedOrderId;

    private String description;

    private LocalDateTime createdAt;
}
