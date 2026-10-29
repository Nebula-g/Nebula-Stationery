package com.nebula.stationery.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/** 模拟短信通道：不联网、不花钱，验证码写日志并存最近 20 条，接口里回显给前端演示 */
@Slf4j
@Component
@ConditionalOnProperty(name = "stationery.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    private final Deque<Map<String, String>> recent = new ArrayDeque<>();

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean mock() {
        return true;
    }

    @Override
    public void sendCode(String phone, String code) {
        log.info("【模拟短信】发往 {}：您的验证码是 {}，5 分钟内有效。", phone, code);
        synchronized (recent) {
            recent.addFirst(Map.of("phone", phone, "code", code, "at", java.time.LocalTime.now().withNano(0).toString()));
            while (recent.size() > 20) {
                recent.removeLast();
            }
        }
    }

    public java.util.List<Map<String, String>> recent() {
        synchronized (recent) {
            return new java.util.ArrayList<>(recent);
        }
    }
}
