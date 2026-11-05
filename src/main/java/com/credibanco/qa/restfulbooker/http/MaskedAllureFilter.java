package com.credibanco.qa.restfulbooker.http;

import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.nio.charset.StandardCharsets;

/**
 * Attaches every request and response to the current Allure step with secrets masked (ADR-05, FW-01).
 * Stateless, so one instance can serve all specs.
 */
public final class MaskedAllureFilter implements OrderedFilter {

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        Response response = context.next(request, responseSpec);
        if (AllureReporter.isReporting()) {
            AllureReporter.attach("Request: " + request.getMethod() + " " + request.getDerivedPath(), describe(request));
            AllureReporter.attach("Response: " + response.getStatusCode() + " (" + response.getTime() + " ms)",
                    describe(response));
        }
        return response;
    }

    /** Last filter before sending: sees the final request, including the token cookie. */
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    private static String describe(FilterableRequestSpecification request) {
        StringBuilder text = new StringBuilder()
                .append(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        for (Header header : request.getHeaders()) {
            text.append(header.getName()).append(": ")
                    .append(SensitiveDataMasker.maskHeader(header.getName(), header.getValue())).append('\n');
        }
        for (Cookie cookie : request.getCookies()) {
            text.append("Cookie: ").append(cookie.getName()).append('=').append(SensitiveDataMasker.MASK).append('\n');
        }
        String body = bodyAsText(request.getBody());
        if (body != null) {
            text.append('\n').append(SensitiveDataMasker.maskBody(body));
        }
        return text.toString();
    }

    private static String describe(Response response) {
        StringBuilder text = new StringBuilder().append(response.getStatusLine()).append('\n');
        for (Header header : response.getHeaders()) {
            text.append(header.getName()).append(": ")
                    .append(SensitiveDataMasker.maskHeader(header.getName(), header.getValue())).append('\n');
        }
        text.append("Time: ").append(response.getTime()).append(" ms\n\n")
                .append(SensitiveDataMasker.maskBody(response.asString()));
        return text.toString();
    }

    private static String bodyAsText(Object body) {
        if (body == null) {
            return null;
        }
        if (body instanceof String text) {
            return text;
        }
        if (body instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        try {
            return JsonMapper.instance().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return "<unserializable body: " + body.getClass().getName() + ">";
        }
    }
}
