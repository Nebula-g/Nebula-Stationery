package com.nebula.stationery.mq;

import java.util.Map;

/**
 * 一条已投递的消息。payload 用字符串键值对承载，和 Redis Stream 的字段模型对齐。
 */
public record Message(String id, String topic, String type, Map<String, String> payload, int retries) {

    public String field(String name) {
        return payload.get(name);
    }

    public long longField(String name) {
        String value = payload.get(name);
        return value == null ? 0L : Long.parseLong(value);
    }

    public Message withRetries(int newRetries) {
        return new Message(id, topic, type, payload, newRetries);
    }
}
