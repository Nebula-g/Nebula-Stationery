package com.nebula.stationery.logistics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 快递100 实时查询（企业版 poll/query.do，sign = MD5(param + key + customer) 大写）。
 * 未配置 key/customer 时 available() 为 false，业务层自动退回模拟轨迹。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Kuaidi100Provider implements LogisticsProvider {

    private static final String ENDPOINT = "https://poll.kuaidi100.com/poll/query.do";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;

    @Value("${stationery.logistics.kuaidi100.key:}")
    private String key;

    @Value("${stationery.logistics.kuaidi100.customer:}")
    private String customer;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public String name() {
        return "kuaidi100";
    }

    @Override
    public boolean available() {
        return key != null && !key.isBlank() && customer != null && !customer.isBlank();
    }

    @Override
    public List<TrackPoint> query(String companyCode, String trackingNo, String receiverPhone, LocalDateTime shippedAt) {
        if (!available()) {
            return List.of();
        }
        try {
            Map<String, String> param = new LinkedHashMap<>();
            param.put("com", companyCode.toLowerCase());
            param.put("num", trackingNo);
            if (receiverPhone != null && receiverPhone.length() >= 4) {
                param.put("phone", receiverPhone.substring(receiverPhone.length() - 4));
            }
            param.put("resultv2", "1");
            String paramJson = objectMapper.writeValueAsString(param);

            Map<String, String> form = new LinkedHashMap<>();
            form.put("customer", customer);
            form.put("sign", md5(paramJson + key + customer).toUpperCase());
            form.put("param", paramJson);

            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(response.body());

            List<TrackPoint> points = new ArrayList<>();
            for (JsonNode item : root.path("data")) {
                String time = item.path("time").asText();
                points.add(new TrackPoint(
                        time.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(time, TIME_FORMAT),
                        item.path("areaName").asText(null),
                        item.path("context").asText()
                ));
            }
            // 快递100 返回顺序是新→旧，统一按时间正序给前端画时间线
            points.sort((a, b) -> a.time().compareTo(b.time()));
            return points;
        } catch (Exception e) {
            log.warn("快递100 查询失败，改用本地轨迹：{}", e.getMessage());
            return List.of();
        }
    }

    private String encodeForm(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 不可用", e);
        }
    }
}
