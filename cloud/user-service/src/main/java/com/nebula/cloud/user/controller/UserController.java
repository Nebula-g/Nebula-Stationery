package com.nebula.cloud.user.controller;

import com.nebula.cloud.common.CurrentUser;
import com.nebula.cloud.common.Result;
import com.nebula.cloud.user.service.UserService;
import com.nebula.cloud.user.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        return Result.success(userService.profile(CurrentUser.id(request)));
    }
}
