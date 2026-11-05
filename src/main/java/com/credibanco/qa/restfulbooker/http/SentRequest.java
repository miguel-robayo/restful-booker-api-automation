package com.credibanco.qa.restfulbooker.http;

import java.util.Optional;

/**
 * A request as it actually left the client: method, path and the fingerprint of the token it carried (never the
 * token itself).
 */
public record SentRequest(String method, String path, Optional<String> tokenFingerprint) {

    @Override
    public String toString() {
        return method + " " + path + " -> " + tokenFingerprint.orElse("no token");
    }
}
