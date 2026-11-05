package com.credibanco.qa.restfulbooker.api;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

/**
 * Base of every API Object: holds the spec it was built with and starts requests from it. API Objects know paths,
 * methods and parameters; they never validate responses (ADR-04).
 */
public abstract class BaseApi {

    private final RequestSpecification spec;

    protected BaseApi(RequestSpecification spec) {
        this.spec = spec;
    }

    /** A new request based on this API's spec; the spec itself is never modified. */
    protected RequestSpecification request() {
        return RestAssured.given(spec);
    }
}
