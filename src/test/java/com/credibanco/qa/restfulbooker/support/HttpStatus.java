package com.credibanco.qa.restfulbooker.support;

/**
 * Expected HTTP status codes, named so that business-language steps never show raw codes (C-2). Values come from
 * the test document and the API contract; they are not changed by the refactoring.
 */
public final class HttpStatus {

    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;

    private HttpStatus() {
    }
}
