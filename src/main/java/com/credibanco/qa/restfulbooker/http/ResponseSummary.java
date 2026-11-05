package com.credibanco.qa.restfulbooker.http;

import io.restassured.response.Response;

public final class ResponseSummary {

    private static final int MAX_BODY_IN_MESSAGE = 300;

    private ResponseSummary() {
    }

    public static String of(Response response) {
        String body = SensitiveDataMasker.maskBody(response.asString());
        return "HTTP " + response.statusCode() + (body == null || body.isBlank() ? "" : " " + truncate(body));
    }

    private static String truncate(String text) {
        return text.length() <= MAX_BODY_IN_MESSAGE ? text : text.substring(0, MAX_BODY_IN_MESSAGE) + "…";
    }
}
