package com.credibanco.qa.restfulbooker.http;

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Sends the auth token as the header {@code Cookie: token=<value>} (RC-05). One instance per authenticated spec; the
 * token is final, so it can never leak between actors running in parallel.
 *
 * <p>Sent as a header, not as a Rest Assured cookie: Rest Assured's conditional log prints cookies in clear text,
 * while the {@code Cookie} header is blacklisted (see {@link SpecFactory}).
 */
public final class TokenCookieFilter implements OrderedFilter {

    private static final String COOKIE_HEADER = "Cookie";
    private static final String COOKIE_NAME = "token";

    private final String token;

    public TokenCookieFilter(String token) {
        this.token = token;
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
                           FilterContext context) {
        request.header(COOKIE_HEADER, COOKIE_NAME + "=" + token);
        return context.next(request, response);
    }

    /** Runs before {@link MaskedAllureFilter}, so the report shows that the cookie was sent (masked). */
    @Override
    public int getOrder() {
        return DEFAULT_PRECEDENCE;
    }

    @Override
    public String toString() {
        return "TokenCookieFilter[token=" + SensitiveDataMasker.MASK + "]";
    }
}
