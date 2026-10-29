package com.nebula.stationery.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis Stream 消息队列：生产端 XADD，消费端用消费组 XREADGROUP + XACK，
 * 失败重试两次后转投死信队列。延迟消息用 ZSET 按投递时间排序，扫描到点后再入队。
 * Redis 不可用时降级为进程内队列，保证没装 Redis 的机器也能跑完整流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueService {

    public static final String TOPIC_ORDER = "stationery:mq:order";
    public static final String TOPIC_REVIEW = "stationery:mq:review";
    public static final String TOPIC_NOTIFY = "stationery:mq:notify";

    private static final String DELAYED_KEY = "stationery:mq:delayed";
    private static final String DLQ_KEY = "stationery:mq:dlq";
    private static final String GROUP = "stationery-group";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_DATA = "data";
    private static final String FIELD_RETRIES = "retries";
    private static final int MAX_RETRY = 2;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(5);
    private static final long PROBE_INTERVAL_MS = 30_000;
    private static final List<String> TOPICS = List.of(TOPIC_ORDER, TOPIC_REVIEW, TOPIC_NOTIFY);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<MessageHandler> handlerProvider;

    private final Map<String, MessageHandler> handlers = new LinkedHashMap<>();
    private final PriorityQueue<LocalMessage> localMessages =
            new PriorityQueue<>(java.util.Comparator.comparingLong(LocalMessage::deliverAt));
    private final Deque<Map<String, String>> recentDeadLetters = new ArrayDeque<>();

    private final AtomicLong publishedCount = new AtomicLong();
    private final AtomicLong consumedCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicLong deadCount = new AtomicLong();

    private volatile boolean healthy = true;
    private volatile long lastProbeAt = System.currentTimeMillis();

    private record LocalMessage(long deliverAt, String topic, String type, Map<String, String> payload, int retries) {
    }

    @PostConstruct
    public void init() {
        handlerProvider.stream().forEach(handler -> {
            MessageHandler previous = handlers.putIfAbsent(handler.supportedType(), handler);
            if (previous != null) {
                throw new IllegalStateException("消息类型 " + handler.supportedType() + " 有重复处理器");
            }
        });
        log.info("消息队列已注册 {} 个处理器：{}", handlers.size(), handlers.keySet());
    }

    public void publish(String topic, String type, Map<String, String> payload) {
        publishDelayed(topic, type, payload, Duration.ZERO, 0);
    }

    /** delay 为订单超时这类「稍后投递」场景服务，0 表示立即可被消费 */
    public void publishDelayed(String topic, String type, Map<String, String> payload, Duration delay) {
        publishDelayed(topic, type, payload, delay, 0);
    }

    private void publishDelayed(String topic, String type, Map<String, String> payload, Duration delay, int retries) {
        publishedCount.incrementAndGet();
        long deliverAt = System.currentTimeMillis() + Math.max(0, delay.toMillis());

        if (!healthy()) {
            synchronized (localMessages) {
                localMessages.add(new LocalMessage(deliverAt, topic, type, payload, retries));
            }
            return;
        }

        try {
            if (deliverAt > System.currentTimeMillis()) {
                redis.opsForZSet().add(DELAYED_KEY, serialize(topic, type, payload, retries), deliverAt);
                return;
            }
            ensureGroup(topic);
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put(FIELD_TYPE, type);
            fields.put(FIELD_DATA, serialize(payload));
            fields.put(FIELD_RETRIES, String.valueOf(retries));
            redis.opsForStream().add(StreamRecords.<String, String>newRecord().in(topic).ofMap(fields));
        } catch (Exception e) {
            degrade(e);
            synchronized (localMessages) {
                localMessages.add(new LocalMessage(deliverAt, topic, type, payload, retries));
            }
        }
    }

    @Scheduled(fixedDelayString = "${stationery.mq.poll-ms:1000}")
    public void poll() {
        if (healthy()) {
            try {
                releaseDueDelayed();
                for (String topic : TOPICS) {
                    consumeTopic(topic);
                }
            } catch (Exception e) {
                degrade(e);
            }
        }
        drainLocal();
    }

    private void releaseDueDelayed() {
        long now = System.currentTimeMillis();
        Set<String> due = redis.opsForZSet().rangeByScore(DELAYED_KEY, 0, now, 0, 50);
        if (due == null || due.isEmpty()) {
            return;
        }
        for (String json : due) {
            redis.opsForZSet().remove(DELAYED_KEY, json);
            LocalMessage message = deserializeLocal(json);
            if (message != null) {
                publishDelayed(message.topic(), message.type(), message.payload(), Duration.ZERO, message.retries());
            }
        }
    }

    private void consumeTopic(String topic) {
        ensureGroup(topic);
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(GROUP, consumerName()),
                StreamReadOptions.empty().count(20),
                StreamOffset.create(topic, ReadOffset.lastConsumed())
        );
        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            Map<Object, Object> value = record.getValue();
            Message message = new Message(
                    record.getId().getValue(),
                    topic,
                    str(value.get(FIELD_TYPE)),
                    deserializePayload(str(value.get(FIELD_DATA))),
                    parseInt(str(value.get(FIELD_RETRIES)))
            );
            dispatch(message);
            redis.opsForStream().acknowledge(topic, GROUP, record.getId());
        }
    }

    private void drainLocal() {
        synchronized (localMessages) {
            long now = System.currentTimeMillis();
            while (!localMessages.isEmpty() && localMessages.peek().deliverAt() <= now) {
                LocalMessage local = localMessages.poll();
                dispatch(new Message("local-" + now, local.topic(), local.type(), local.payload(), local.retries()));
            }
        }
    }

    private void dispatch(Message message) {
        MessageHandler handler = handlers.get(message.type());
        if (handler == null) {
            log.warn("消息类型 {} 没有处理器，直接丢弃", message.type());
            return;
        }
        try {
            handler.handle(message);
            consumedCount.incrementAndGet();
        } catch (Exception e) {
            failedCount.incrementAndGet();
            if (message.retries() < MAX_RETRY) {
                log.warn("消息 {} 处理失败，{} 秒后第 {} 次重试：{}",
                        message.type(), RETRY_DELAY.toSeconds(), message.retries() + 1, e.getMessage());
                publishDelayed(message.topic(), message.type(), message.payload(), RETRY_DELAY, message.retries() + 1);
            } else {
                moveToDeadLetter(message, e);
            }
        }
    }

    private void moveToDeadLetter(Message message, Exception e) {
        deadCount.incrementAndGet();
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("type", message.type());
        entry.put("topic", message.topic());
        entry.put("payload", String.valueOf(message.payload()));
        entry.put("retries", String.valueOf(message.retries()));
        entry.put("error", String.valueOf(e.getMessage()));
        entry.put("at", java.time.LocalDateTime.now().toString());
        log.error("消息 {} 重试耗尽，转入死信队列：{}", message.type(), e.getMessage());

        synchronized (recentDeadLetters) {
            recentDeadLetters.addFirst(entry);
            while (recentDeadLetters.size() > 20) {
                recentDeadLetters.removeLast();
            }
        }
        if (!healthy()) {
            return;
        }
        try {
            redis.opsForStream().add(StreamRecords.<String, String>newRecord().in(DLQ_KEY).ofMap(entry));
        } catch (Exception ex) {
            degrade(ex);
        }
    }

    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("backend", healthy ? "redis-stream" : "in-memory(降级)");
        stats.put("published", publishedCount.get());
        stats.put("consumed", consumedCount.get());
        stats.put("failed", failedCount.get());
        stats.put("dead", deadCount.get());
        stats.put("localPending", localMessages.size());

        Map<String, Object> streams = new LinkedHashMap<>();
        long delayed = 0;
        long dlq = 0;
        if (healthy()) {
            try {
                for (String topic : TOPICS) {
                    Map<String, Object> info = new LinkedHashMap<>();
                    Long length = redis.opsForStream().size(topic);
                    info.put("length", length == null ? 0 : length);
                    ensureGroup(topic);
                    info.put("groupPending", redis.opsForStream().pending(topic, GROUP).getTotalPendingMessages());
                    streams.put(shortName(topic), info);
                }
                Long delayedCount = redis.opsForZSet().size(DELAYED_KEY);
                delayed = delayedCount == null ? 0 : delayedCount;
                Long dlqCount = redis.opsForStream().size(DLQ_KEY);
                dlq = dlqCount == null ? 0 : dlqCount;
            } catch (Exception e) {
                degrade(e);
                stats.put("backend", "in-memory(降级)");
            }
        }
        stats.put("streams", streams);
        stats.put("delayed", delayed);
        stats.put("deadLetterLength", dlq);
        synchronized (recentDeadLetters) {
            stats.put("recentDeadLetters", List.copyOf(recentDeadLetters));
        }
        return stats;
    }

    private void ensureGroup(String topic) {
        try {
            redis.opsForStream().createGroup(topic, ReadOffset.from("0"), GROUP);
        } catch (Exception ignored) {
            // BUSYGROUP：消费组已存在，属于正常情况
        }
    }

    private String consumerName() {
        return "consumer-1";
    }

    private String shortName(String topic) {
        return topic.substring(topic.lastIndexOf(':') + 1);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("消息序列化失败", e);
        }
    }

    private String serialize(String topic, String type, Map<String, String> payload, int retries) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("deliverAt", 0);
        wrapper.put("topic", topic);
        wrapper.put("type", type);
        wrapper.put("payload", payload);
        wrapper.put("retries", retries);
        return serialize(wrapper);
    }

    private LocalMessage deserializeLocal(String json) {
        try {
            Map<String, Object> wrapper = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
            Map<String, String> payload = new LinkedHashMap<>();
            Object raw = wrapper.get("payload");
            if (raw instanceof Map<?, ?> map) {
                map.forEach((k, v) -> payload.put(String.valueOf(k), String.valueOf(v)));
            }
            return new LocalMessage(0,
                    String.valueOf(wrapper.get("topic")),
                    String.valueOf(wrapper.get("type")),
                    payload,
                    parseInt(String.valueOf(wrapper.get("retries"))));
        } catch (Exception e) {
            log.warn("延迟消息反序列化失败，丢弃：{}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> deserializePayload(String json) {
        Map<String, String> payload = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return payload;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });
            raw.forEach((k, v) -> payload.put(k, String.valueOf(v)));
        } catch (Exception e) {
            log.warn("消息体解析失败：{}", e.getMessage());
        }
        return payload;
    }

    private int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean healthy() {
        if (healthy) {
            return true;
        }
        if (System.currentTimeMillis() - lastProbeAt < PROBE_INTERVAL_MS) {
            return false;
        }
        lastProbeAt = System.currentTimeMillis();
        try {
            redis.hasKey("__mq_probe__");
            healthy = true;
            log.info("Redis 恢复，消息队列切回 Redis Stream");
        } catch (Exception e) {
            return false;
        }
        return healthy;
    }

    private void degrade(Exception e) {
        if (healthy) {
            healthy = false;
            lastProbeAt = System.currentTimeMillis();
            log.warn("Redis 不可用，消息队列降级为进程内实现（{} 秒后重试）：{}", PROBE_INTERVAL_MS / 1000, e.getMessage());
        }
    }
}
