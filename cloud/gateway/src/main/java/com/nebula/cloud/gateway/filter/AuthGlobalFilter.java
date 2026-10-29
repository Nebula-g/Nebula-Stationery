package com.nebula.cloud.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关统一鉴权：白名单直接放行，其余请求必须带有效 JWT；
 * 校验通过后把用户信息塞进请求头，下游服务不再解析令牌。
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() == null ? "GET" : request.getMethod().name();

        // 先清掉客户端自己伪造的用户头，避免越权
        ServerHttpRequest.Builder builder = request.mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-Username");
                    headers.remove("X-User-Role");
                });

        if (isPublic(path, method)) {
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return reject(exchange, "未登录，请先登录");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token.substring(7))
                    .getPayload();

            String userId = String.valueOf(claims.get("userId"));
            String username = String.valueOf(claims.getSubject());
            String role = String.valueOf(claims.get("role"));

            if (path.startsWith("/api/admin/") && !"1".equals(role)) {
                return reject(exchange, "无权限访问");
            }

            builder.header("X-User-Id", userId)
                    .header("X-Username", username)
                    .header("X-User-Role", role);
            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (Exception e) {
            log.warn("令牌校验失败：{}", e.getMessage());
            return reject(exchange, "登录已过期，请重新登录");
        }
    }

    private boolean isPublic(String path, String method) {
        if ("OPTIONS".equals(method)) {
            return true;
        }
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.startsWith("/api/reviews/product/") && "GET".equals(method)) {
            return true;
        }
        if ("GET".equals(method) && (path.startsWith("/api/products") || path.startsWith("/api/categories"))) {
            return true;
        }
        return false;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
