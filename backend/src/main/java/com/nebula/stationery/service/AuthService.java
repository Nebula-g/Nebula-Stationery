package com.nebula.stationery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.dto.LoginRequest;
import com.nebula.stationery.dto.RegisterRequest;
import com.nebula.stationery.entity.User;
import com.nebula.stationery.mapper.UserMapper;
import com.nebula.stationery.utils.JwtUtil;
import com.nebula.stationery.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SmsCodeService smsCodeService;

    public void register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        String phone = trimToNull(request.getPhone());
        if (phone != null) {
            smsCodeService.verify(phone, request.getCode());
            if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone)) > 0) {
                throw new BusinessException("该手机号已注册");
            }
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(phone);
        user.setRole(0); // 普通用户
        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        return buildResponse(user);
    }

    /** 手机验证码登录：号码没注册过就自动注册，省掉「先注册再登录」两步 */
    @Transactional
    public LoginResponse loginByPhone(String phone, String code) {
        smsCodeService.verify(phone, code);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone)
        );
        if (user == null) {
            user = new User();
            user.setUsername(availableUsername(phone));
            // 随机密码：账号只能用验证码登录，避免空密码被撞库
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setPhone(phone);
            user.setRole(0);
            userMapper.insert(user);
        }

        return buildResponse(user);
    }

    private LoginResponse buildResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), String.valueOf(user.getRole()));
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRole(user.getRole());
        response.setUsername(user.getUsername());
        return response;
    }

    private String availableUsername(String phone) {
        String username = phone;
        int suffix = 1;
        while (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0) {
            username = phone + "_" + suffix++;
        }
        return username;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
