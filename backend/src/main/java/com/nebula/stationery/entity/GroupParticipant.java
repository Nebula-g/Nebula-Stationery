package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_participant")
public class GroupParticipant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupSessionId;

    private Long userId;

    private Integer isLeader;

    private Long orderId;

    private LocalDateTime joinedAt;
}
