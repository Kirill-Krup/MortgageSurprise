package com.bank.simulator.service.impl;

import com.bank.simulator.config.TrustProperties;
import com.bank.simulator.service.DocumentHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DocumentHashServiceImpl implements DocumentHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final TrustProperties trustProperties;

    @Override
    public String hash(String documentNumber) {
        if (documentNumber == null || documentNumber.isBlank()) {
            throw new IllegalArgumentException("Document number cannot be empty");
        }
        String normalized = normalize(documentNumber);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            byte[] keyBytes = trustProperties.hmacSecret().getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 algorithm is not available", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Invalid HMAC key configuration", e);
        }
    }

    @Override
    public String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Raw document string cannot be null");
        }
        return raw.replaceAll("[\\s-]+", "").toUpperCase().trim();
    }
}
