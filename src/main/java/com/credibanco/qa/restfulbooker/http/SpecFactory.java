package com.credibanco.qa.restfulbooker.http;

import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * The only place where request specifications are built (RNF-08, ADR-02). Every call returns a brand-new spec, so
 * no specification is ever shared or mutated between actors; static {@code RestAssured.*} state is never touched.
 *
 * <p>Every spec carries: base URL, JSON {@code Accept}/{@code Content-Type} (RC-06), the framework's
 * {@link JsonMapper}, timeouts, request/response logging only when a validation fails (with credential headers
 * blacklisted) and the {@link MaskedAllureFilter}.
 */
public final class SpecFactory {

    private static final MaskedAllureFilter ALLURE_FILTER = new MaskedAllureFilter();
    /** Rest Assured's conditional log, with secrets in bodies masked line by line. */
    private static final PrintStream MASKED_CONSOLE =
            new PrintStream(new MaskingOutputStream(System.out), true, StandardCharsets.UTF_8);
    private static final String CONNECTION_TIMEOUT = "http.connection.timeout";
    private static final String SOCKET_TIMEOUT = "http.socket.timeout";
    private static final String JSON_MEDIA_TYPE = "application/json";

    private final String baseUrl;
    private final RestAssuredConfig restAssuredConfig;

    public SpecFactory(EnvironmentConfig config) {
        this.baseUrl = config.baseUrl().toString();
        this.restAssuredConfig = RestAssuredConfig.config()
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> JsonMapper.instance()))
                .logConfig(LogConfig.logConfig()
                        .defaultStream(MASKED_CONSOLE)
                        .enableLoggingOfRequestAndResponseIfValidationFails()
                        .blacklistHeader("Cookie", "Set-Cookie", "Authorization"))
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam(CONNECTION_TIMEOUT, config.httpTimeoutMs())
                        .setParam(SOCKET_TIMEOUT, config.httpTimeoutMs()));
    }

    /** Unauthenticated spec. */
    public RequestSpecification base() {
        return newBuilder().build();
    }

    /** Spec that sends {@code Cookie: token=<token>}. */
    public RequestSpecification authenticated(String token) {
        return newBuilder().addFilter(new TokenCookieFilter(token)).build();
    }

    private RequestSpecBuilder newBuilder() {
        return new RequestSpecBuilder()
                .setBaseUri(baseUrl)
                .setContentType(ContentType.JSON)
                // Exactly the documented value (RC-06); ContentType.JSON would send four JSON-like media types
                .setAccept(JSON_MEDIA_TYPE)
                .setConfig(restAssuredConfig)
                .addFilter(ALLURE_FILTER);
    }
}
