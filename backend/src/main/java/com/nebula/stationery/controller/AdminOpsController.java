package com.nebula.stationery.controller;

import com.nebula.stationery.cache.CacheService;
import com.nebula.stationery.common.Result;
import com.nebula.stationery.mq.MessageQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维观测：缓存命中率与消息队列堆积情况。
 */
@RestController
@RequestMapping("/api/admin/ops")
@RequiredArgsConstructor
public class AdminOpsController {

    private final CacheService cacheService;
    private final MessageQueueService messageQueueService;

    @GetMapping("/cache")
    public Result<Map<String, Object>> cacheStats() {
        return Result.success(cacheService.stats());
    }

    @DeleteMapping("/cache")
    public Result<Map<String, Object>> clearCache() {
        int removed = cacheService.flush();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("removed", removed);
        return Result.success(data);
    }

    @GetMapping("/queue")
    public Result<Map<String, Object>> queueStats() {
        return Result.success(messageQueueService.stats());
    }
}
