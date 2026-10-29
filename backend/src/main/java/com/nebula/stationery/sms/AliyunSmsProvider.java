package com.nebula.stationery.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nebula.stationery.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 阿里云短信（dysmsapi RPC 风格签名，HMAC-SHA1）。
 * 需要配置 AccessKey、签名名称和模板 CODE，模板变量固定为 ${code}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stationery.sms.provider", havingValue = "aliyun")
public class AliyunSmsProvider implements SmsProvider {

    private static final String ENDPOINT = "https://dysmsapi.aliyuncs.com/";

    private final ObjectMapper objectMapper;

    @Value("${stationery.sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${stationery.sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${stationery.sms.aliyun.sign-name:}")
    private String signName;

    @Value("${stationery.sms.aliyun.template-code:}")
    private String templateCode;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Override
    public String name() {
        return "aliyun";
    }

    @Override
    public boolean mock() {
        return false;
    }

    @Override
    public void sendCode(String phone, String code) {
        if (isBlank(accessKeyId) || isBlank(accessKeySecret) || isBlank(signName) || isBlank(templateCode)) {
            throw new BusinessException("短信服务未配置 AccessKey / 签名 / 模板，请改用 mock 通道");
        }

        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", "SendSms");
        params.put("Format", "JSON");
        params.put("PhoneNumbers", phone);
        params.put("RegionId", "cn-hangzhou");
        params.put("SignName", signName);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", java.util.UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("TemplateCode", templateCode);
        params.put("TemplateParam", "{\"code\":\"" + code + "\"}");
        params.put("Timestamp", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
        params.put("Version", "2017-05-25");

        try {
            String query = params.entrySet().stream()
                    .map(e -> percentEncode(e.getKey()) + "=" + percentEncode(e.getValue()))
                    .collect(Collectors.joining("&"));
            String signature = sign("GET&%2F&" + percentEncode(query));
            String url = ENDPOINT + "?Signature=" + percentEncode(signature) + "&" + query;

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode body = objectMapper.readTree(response.body());
            String resultCode = body.path("Code").asText();
            if (!"OK".equals(resultCode)) {
                throw new BusinessException("短信发送失败：" + body.path("Message").asText(resultCode));
            }
            log.info("阿里云短信已发送至 {}", mask(phone));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("阿里云短信发送异常：{}", e.getMessage());
            throw new BusinessException("短信发送失败，请稍后再试");
        }
    }

    private String sign(String toSign) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        return Base64.getEncoder().encodeToString(mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8)));
    }

    private String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String mask(String phone) {
        return phone.length() < 7 ? phone : phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
