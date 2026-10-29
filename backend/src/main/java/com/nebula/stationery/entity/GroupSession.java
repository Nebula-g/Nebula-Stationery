package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_session")
public class GroupSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupBuyId;

    private Long productId;

    private Long leaderId;

    private Integer currentSize;

    private Integer minSize;

    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;
}
