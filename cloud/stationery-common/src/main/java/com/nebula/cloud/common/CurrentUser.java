package com.nebula.cloud.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 网关校验 JWT 后把用户信息放进请求头，下游服务直接取用，
 * 不再重复解析令牌。
 */
public final class CurrentUser {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_ROLE = "X-User-Role";

    private CurrentUser() {
    }

    public static Long id(HttpServletRequest request) {
        String value = request.getHeader(HEADER_USER_ID);
        if (value == null || value.isBlank()) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        return Long.valueOf(value);
    }

    public static String username(HttpServletRequest request) {
        return request.getHeader(HEADER_USERNAME);
    }

    public static boolean admin(HttpServletRequest request) {
        return "1".equals(request.getHeader(HEADER_ROLE));
    }
}
