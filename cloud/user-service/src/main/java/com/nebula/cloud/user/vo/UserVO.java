package com.nebula.cloud.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;

    private String username;

    private String phone;

    private Integer role;

    private LocalDateTime createdAt;
}
