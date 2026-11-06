package com.credibanco.qa.restfulbooker.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Removes secrets before anything is logged or attached to a report (FW-01). Pure functions, no I/O.
 */
public final class SensitiveDataMasker {

    public static final String MASK = "***";

    private static final Set<String> SENSITIVE_FIELDS = Set.of("password", "token");
    private static final Set<String> SENSITIVE_HEADERS = Set.of("cookie", "set-cookie", "authorization");
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile("(?i)\\b(password|token)=[^;&\\s]+");
    private static final Pattern JSON_SECRET = Pattern.compile("(?i)(\"(?:password|token)\"\\s*:\\s*)\"[^\"]*\"");

    private SensitiveDataMasker() {
    }

    /** Masks sensitive fields at any depth of a JSON body; other text has {@code token=}/{@code password=} masked. */
    public static String maskBody(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            JsonNode tree = JsonMapper.instance().readTree(body);
            if (tree.isContainerNode()) {
                maskNode(tree);
                return tree.toString();
            }
        } catch (JsonProcessingException notJson) {
            // fall through to the plain-text rule
        }
        return KEY_VALUE_SECRET.matcher(body).replaceAll("$1=" + MASK);
    }

    /**
     * Masks secrets in one line of free text, e.g. a line of Rest Assured's log: JSON pairs such as
     * {@code "password": "x"} and {@code token=x} / {@code password=x}.
     */
    public static String maskLine(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        String jsonMasked = JSON_SECRET.matcher(line).replaceAll("$1\"" + MASK + "\"");
        return KEY_VALUE_SECRET.matcher(jsonMasked).replaceAll("$1=" + MASK);
    }

    public static boolean isSensitiveHeader(String name) {
        return name != null && SENSITIVE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    public static String maskHeader(String name, String value) {
        return isSensitiveHeader(name) ? MASK : value;
    }

    private static void maskNode(JsonNode node) {
        if (node instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (SENSITIVE_FIELDS.contains(field.getKey().toLowerCase(Locale.ROOT))) {
                    field.setValue(TextNode.valueOf(MASK));
                } else {
                    maskNode(field.getValue());
                }
            }
        } else if (node instanceof ArrayNode array) {
            array.forEach(SensitiveDataMasker::maskNode);
        }
    }
}
