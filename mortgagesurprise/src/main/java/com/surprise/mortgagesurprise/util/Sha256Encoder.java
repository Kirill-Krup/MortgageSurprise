package com.surprise.mortgagesurprise.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Sha256Encoder {

    private final static String ALGORITHM =  "SHA-256";

    private Sha256Encoder() {}

    public static String toSha256(String input) {
        try {
            var hash = MessageDigest.getInstance(ALGORITHM)
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
