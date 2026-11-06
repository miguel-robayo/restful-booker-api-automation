package com.credibanco.qa.restfulbooker.http;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.net.URI;
import java.util.function.Consumer;

/**
 * Reports each request exactly as it is sent, including the fingerprint of the token in its final {@code Cookie}
 * header. Runs after {@link TokenCookieFilter}, so it observes what really goes on the wire.
 */
public final class SentRequestFilter implements OrderedFilter {

    private static final String COOKIE = "Cookie";

    private final Consumer<SentRequest> listener;

    public SentRequestFilter(Consumer<SentRequest> listener) {
        this.listener = listener;
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
                           FilterContext context) {
        listener.accept(new SentRequest(request.getMethod(), URI.create(request.getURI()).getPath(),
                TokenFingerprint.fromCookieHeader(request.getHeaders().getValue(COOKIE))));
        return context.next(request, response);
    }

    /** After the token filter, before the Allure filter. */
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 1;
    }
}
