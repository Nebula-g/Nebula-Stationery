package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("logistics_track")
public class LogisticsTrack {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long logisticsId;

    private LocalDateTime trackTime;

    private String description;

    private String location;
}
