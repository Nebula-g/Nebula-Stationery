package com.nebula.cloud.user.vo;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;

    private Integer role;

    private String username;
}
