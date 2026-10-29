package com.nebula.stationery.vo;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private Integer role;
    private String username;
}
