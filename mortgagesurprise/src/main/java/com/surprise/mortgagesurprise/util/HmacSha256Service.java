package com.surprise.mortgagesurprise.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Сервис криптографического хэширования с ключом (HMAC-SHA256).
 * Защищает паспортные данные от атак по радужным таблицам и прямого перебора.
 */
@Component
public class HmacSha256Service {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] keyBytes;

    public HmacSha256Service(
            @Value("${app.security.passport-hmac-key:${DOC_HMAC_SECRET:v8y/B?E(H+MbQeThWmZq4t7w!z%C*F-J}}") String rawKey
    ) {
        this.keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 hash", e);
        }
    }
}
