package com.nebula.stationery.service;

import com.nebula.stationery.cache.CacheService;
import com.nebula.stationery.common.BusinessException;
import com.nebula.stationery.sms.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证码的生成、限流与校验。验证码存 Redis（5 分钟），同号码 60 秒内不能重复发送；
 * Redis 不可用时 CacheService 自动改用进程内存储。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final String CODE_PREFIX = "stationery:sms:code:";
    private static final String SENT_PREFIX = "stationery:sms:sent:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CacheService cacheService;
    private final SmsProvider smsProvider;

    public Map<String, Object> send(String phone) {
        checkPhone(phone);
        String sentKey = SENT_PREFIX + phone;
        if (cacheService.getRaw(sentKey) != null) {
            throw new BusinessException("验证码已发送，请 60 秒后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        cacheService.putRaw(CODE_PREFIX + phone, code, CODE_TTL);
        cacheService.putRaw(sentKey, "1", RESEND_INTERVAL);
        smsProvider.sendCode(phone, code);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", smsProvider.name());
        result.put("expiresIn", CODE_TTL.toSeconds());
        result.put("resendIn", RESEND_INTERVAL.toSeconds());
        // 模拟通道直接把验证码回显，方便课堂演示；真实通道不会泄露
        result.put("mock", smsProvider.mock());
        if (smsProvider.mock()) {
            result.put("devCode", code);
        }
        return result;
    }

    public void verify(String phone, String code) {
        checkPhone(phone);
        if (code == null || code.isBlank()) {
            throw new BusinessException("请输入短信验证码");
        }
        String key = CODE_PREFIX + phone;
        String cached = cacheService.getRaw(key);
        if (cached == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!cached.equals(code.trim())) {
            throw new BusinessException("验证码不正确");
        }
        // 一次性使用
        cacheService.evict(key);
    }

    private void checkPhone(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入正确的 11 位手机号");
        }
    }
}
