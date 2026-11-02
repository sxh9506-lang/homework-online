package com.homework.online.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 轻量 JWT（HS256）实现，只依赖 JDK 与 Jackson。
 * 令牌载荷：{ "uid": 用户ID, "exp": 过期时间戳(毫秒) }
 */
@Service
public class JwtService {

    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKeySpec keySpec;
    private final long ttlMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-hours:168}") long ttlHours) {
        this.keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.ttlMillis = ttlHours * 60L * 60L * 1000L;
    }

    public String generate(Long userId) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("uid", userId);
            payload.put("exp", System.currentTimeMillis() + ttlMillis);

            String header = ENCODER.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
            String body = ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = sign(header + "." + body);
            return header + "." + body + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("生成令牌失败", e);
        }
    }

    /** 校验签名与有效期，返回用户 ID；失败抛出 IllegalArgumentException */
    public Long parseUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("令牌格式不正确");
            }
            String expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("令牌签名不匹配");
            }
            JsonNode payload = objectMapper.readTree(DECODER.decode(parts[1]));
            long exp = payload.path("exp").asLong();
            if (exp < System.currentTimeMillis()) {
                throw new IllegalArgumentException("令牌已过期");
            }
            return payload.path("uid").asLong();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("令牌解析失败", e);
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("签名失败", e);
        }
    }
}
