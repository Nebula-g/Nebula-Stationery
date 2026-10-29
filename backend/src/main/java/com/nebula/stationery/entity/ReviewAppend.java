package com.nebula.stationery.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_append")
public class ReviewAppend {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reviewId;

    private Long userId;

    private String content;

    private String imageUrls;

    private LocalDateTime createdAt;
}
