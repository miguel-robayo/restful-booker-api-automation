package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.http.JsonMapper;
import com.credibanco.qa.restfulbooker.http.SensitiveDataMasker;
import com.credibanco.qa.restfulbooker.model.AuthRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FW-01: secrets never reach logs or report attachments. No network.
 */
@DisplayName("FW-01 Sensitive data masking")
class SensitiveDataMaskerTest {

    @Test
    @DisplayName("masks the password of an auth request and keeps the rest")
    void masksPasswordInBody() throws IOException {
        String body = JsonMapper.instance().writeValueAsString(
                AuthRequest.builder().username("user-x").password("secret-x").build());

        JsonNode masked = JsonMapper.instance().readTree(SensitiveDataMasker.maskBody(body));

        assertAll(
                () -> assertEquals("user-x", masked.get("username").asText()),
                () -> assertEquals(SensitiveDataMasker.MASK, masked.get("password").asText()));
    }

    @Test
    @DisplayName("masks sensitive fields at any depth, case-insensitively, and keeps the JSON structure")
    void masksNestedFields() throws IOException {
        ObjectNode root = JsonMapper.instance().createObjectNode();
        root.putObject("session").put("Token", "abc123").put("user", "user-x");
        root.putArray("items").addObject().put("password", "secret-x");

        JsonNode masked = JsonMapper.instance().readTree(SensitiveDataMasker.maskBody(root.toString()));

        assertAll(
                () -> assertEquals(SensitiveDataMasker.MASK, masked.at("/session/Token").asText()),
                () -> assertEquals("user-x", masked.at("/session/user").asText()),
                () -> assertEquals(SensitiveDataMasker.MASK, masked.at("/items/0/password").asText()));
    }

    @Test
    @DisplayName("masks token and password in non-JSON text")
    void masksKeyValueText() {
        String masked = SensitiveDataMasker.maskBody("token=abc123; Path=/; password=secret-x&user=user-x");

        assertAll(
                () -> assertFalse(masked.contains("abc123")),
                () -> assertFalse(masked.contains("secret-x")),
                () -> assertTrue(masked.contains("user=user-x")));
    }

    @Test
    @DisplayName("masks credential headers regardless of case and leaves others intact")
    void masksSensitiveHeaders() {
        assertAll(
                () -> assertEquals(SensitiveDataMasker.MASK, SensitiveDataMasker.maskHeader("Cookie", "token=abc123")),
                () -> assertEquals(SensitiveDataMasker.MASK, SensitiveDataMasker.maskHeader("set-cookie", "token=abc")),
                () -> assertEquals(SensitiveDataMasker.MASK, SensitiveDataMasker.maskHeader("AUTHORIZATION", "Basic x")),
                () -> assertEquals("application/json", SensitiveDataMasker.maskHeader("Content-Type", "application/json")));
    }

    @Test
    @DisplayName("masks secrets in single log lines, including pretty-printed JSON pairs")
    void masksLogLines() {
        assertAll(
                () -> assertEquals("    \"password\": \"***\",", SensitiveDataMasker.maskLine("    \"password\": \"secret-x\",")),
                () -> assertEquals("{\"token\":\"***\"}", SensitiveDataMasker.maskLine("{\"token\":\"abc123\"}")),
                () -> assertEquals("Cookies:\t\ttoken=***", SensitiveDataMasker.maskLine("Cookies:\t\ttoken=abc123")),
                () -> assertEquals("Request method:\tPUT", SensitiveDataMasker.maskLine("Request method:\tPUT")));
    }

    @Test
    @DisplayName("masks every line written to the log stream, also from parallel threads")
    void masksLogStreamAcrossThreads() throws Exception {
        java.io.ByteArrayOutputStream console = new java.io.ByteArrayOutputStream();
        java.io.PrintStream log = new java.io.PrintStream(
                new com.credibanco.qa.restfulbooker.http.MaskingOutputStream(console), true,
                java.nio.charset.StandardCharsets.UTF_8);

        java.util.stream.IntStream.range(0, 200).parallel()
                .forEach(i -> log.println("{\"password\": \"secret-" + i + "\", \"username\": \"user-" + i + "\"}"));

        String output = console.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertAll(
                () -> assertFalse(output.contains("secret-"), "no password may reach the console"),
                () -> assertEquals(200, output.lines().filter(l -> l.contains("\"password\": \"***\"")).count()),
                () -> assertTrue(output.contains("user-199")));
    }

    @Test
    @DisplayName("leaves empty and secret-free bodies unchanged")
    void leavesHarmlessBodiesUnchanged() {
        assertAll(
                () -> assertNull(SensitiveDataMasker.maskBody(null)),
                () -> assertEquals("", SensitiveDataMasker.maskBody("")),
                () -> assertEquals("Not Found", SensitiveDataMasker.maskBody("Not Found")),
                () -> assertEquals("{\"bookingid\":1}", SensitiveDataMasker.maskBody("{\"bookingid\":1}")));
    }
}
