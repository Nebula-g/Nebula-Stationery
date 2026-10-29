package com.nebula.cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nebula.cloud.common.BusinessException;
import com.nebula.cloud.common.JwtUtil;
import com.nebula.cloud.user.dto.LoginRequest;
import com.nebula.cloud.user.dto.RegisterRequest;
import com.nebula.cloud.user.entity.User;
import com.nebula.cloud.user.mapper.UserMapper;
import com.nebula.cloud.user.vo.LoginResponse;
import com.nebula.cloud.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("用户名已存在");
        }

        String phone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(phone);
        user.setRole(0);
        userMapper.insert(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        LoginResponse response = new LoginResponse();
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), String.valueOf(user.getRole())));
        response.setRole(user.getRole());
        response.setUsername(user.getUsername());
        return response;
    }

    public UserVO profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    public String username(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? "匿名用户" : user.getUsername();
    }
}
