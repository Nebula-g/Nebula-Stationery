package com.nebula.stationery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 选填：填了手机号就必须带短信验证码 */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    private String code;
}
