package com.credibanco.qa.restfulbooker.http;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * One-way identity of a token: lets reports and assertions compare tokens without revealing them.
 * {@code sha256:} plus the first 16 hex characters (64 bits) of the SHA-256 digest.
 */
public final class TokenFingerprint {

    private static final String PREFIX = "sha256:";
    private static final int HEX_LENGTH = 16;
    private static final String TOKEN_COOKIE = "token=";

    private TokenFingerprint() {
    }

    public static String of(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return PREFIX + HexFormat.of().formatHex(digest).substring(0, HEX_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }

    /** Fingerprint of the {@code token} cookie inside a {@code Cookie} header value, if present. */
    public static Optional<String> fromCookieHeader(String cookieHeader) {
        if (cookieHeader == null) {
            return Optional.empty();
        }
        for (String cookie : cookieHeader.split(";")) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith(TOKEN_COOKIE) && trimmed.length() > TOKEN_COOKIE.length()) {
                return Optional.of(of(trimmed.substring(TOKEN_COOKIE.length())));
            }
        }
        return Optional.empty();
    }
}
