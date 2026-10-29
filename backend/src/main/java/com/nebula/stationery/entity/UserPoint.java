package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_point")
public class UserPoint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer totalPoints;

    private Integer availablePoints;

    private Integer usedPoints;

    private LocalDateTime updatedAt;
}
