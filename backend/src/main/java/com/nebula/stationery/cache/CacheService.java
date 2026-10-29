package com.nebula.stationery.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Redis 缓存门面：Redis 连不上时自动降级为进程内缓存，并每隔 30 秒重新探测，
 * 这样没装 Redis 的机器也能正常启动和使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final long PROBE_INTERVAL_MS = 30_000;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private final Map<String, LocalEntry> localCache = new ConcurrentHashMap<>();
    private final AtomicLong hitCount = new AtomicLong();
    private final AtomicLong missCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();

    private volatile boolean healthy = true;
    private volatile long lastProbeAt = System.currentTimeMillis();

    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        String cached = read(key);
        if (cached != null) {
            try {
                T value = objectMapper.readValue(cached, type);
                hitCount.incrementAndGet();
                return value;
            } catch (Exception e) {
                log.warn("缓存 {} 反序列化失败，回源重建：{}", key, e.getMessage());
                evict(key);
            }
        }

        missCount.incrementAndGet();
        T value = loader.get();
        if (value != null) {
            write(key, value, ttl);
        }
        return value;
    }

    public void evict(String key) {
        localCache.remove(key);
        if (healthy()) {
            try {
                redis.delete(key);
            } catch (Exception e) {
                degrade(e);
            }
        }
    }

    /** 原样存取短字符串（验证码、限流标记这类，不需要 JSON 包装） */
    public String getRaw(String key) {
        return read(key);
    }

    public void putRaw(String key, String value, Duration ttl) {
        localCache.put(key, new LocalEntry(value, System.currentTimeMillis() + ttl.toMillis()));
        if (!healthy()) {
            return;
        }
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            degrade(e);
        }
    }

    /** 前缀失效：商品列表、联想结果这类按参数分键的缓存在写操作后整体清掉 */
    public void evictPrefix(String prefix) {
        localCache.keySet().removeIf(k -> k.startsWith(prefix));
        if (!healthy()) {
            return;
        }
        try {
            List<String> keys = new ArrayList<>();
            try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(prefix + "*").count(200).build())) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception e) {
            degrade(e);
        }
    }

    /** 清空本项目所有缓存，返回清除的键数量 */
    public int flush() {
        int removed = localCache.size();
        localCache.clear();
        if (!healthy()) {
            return removed;
        }
        try {
            List<String> keys = new ArrayList<>();
            try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match("stationery:*").count(200).build())) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
                removed = Math.max(removed, keys.size());
            }
        } catch (Exception e) {
            degrade(e);
        }
        return removed;
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        long hit = hitCount.get();
        long miss = missCount.get();
        stats.put("backend", healthy ? "redis" : "in-memory(降级)");
        stats.put("hit", hit);
        stats.put("miss", miss);
        stats.put("hitRate", hit + miss == 0 ? "0.0%" : String.format("%.1f%%", hit * 100.0 / (hit + miss)));
        stats.put("failure", failureCount.get());
        stats.put("localEntries", localCache.size());
        return stats;
    }

    private String read(String key) {
        LocalEntry local = localCache.get(key);
        if (!healthy()) {
            return local != null && !local.expired() ? local.value : null;
        }
        try {
            String value = redis.opsForValue().get(key);
            if (value != null) {
                localCache.remove(key);
            }
            return value;
        } catch (Exception e) {
            degrade(e);
            return local != null && !local.expired() ? local.value : null;
        }
    }

    private void write(String key, Object value, Duration ttl) {
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("缓存 {} 序列化失败，跳过缓存：{}", key, e.getMessage());
            return;
        }

        localCache.put(key, new LocalEntry(json, System.currentTimeMillis() + ttl.toMillis()));
        if (!healthy()) {
            return;
        }
        try {
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            degrade(e);
        }
    }

    private void degrade(Exception e) {
        failureCount.incrementAndGet();
        if (healthy) {
            healthy = false;
            lastProbeAt = System.currentTimeMillis();
            log.warn("Redis 不可用，缓存降级为进程内实现（{} 秒后重试）：{}", PROBE_INTERVAL_MS / 1000, e.getMessage());
        }
    }

    /**
     * 降级状态下周期性探测 Redis 是否恢复。
     * 放在读路径里判断，省掉一个定时任务。
     */
    private boolean healthy() {
        if (healthy) {
            return true;
        }
        if (System.currentTimeMillis() - lastProbeAt < PROBE_INTERVAL_MS) {
            return false;
        }
        lastProbeAt = System.currentTimeMillis();
        try {
            redis.hasKey("__probe__");
            healthy = true;
            log.info("Redis 已恢复，缓存切回 Redis");
        } catch (Exception e) {
            return false;
        }
        return healthy;
    }

    private record LocalEntry(String value, long expireAt) {
        boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
