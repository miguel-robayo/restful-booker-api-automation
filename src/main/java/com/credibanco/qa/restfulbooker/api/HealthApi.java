package com.credibanco.qa.restfulbooker.api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * {@code /ping} health check (RC-07), used only to verify the environment before a run.
 */
public final class HealthApi extends BaseApi {

    private static final String PING = "/ping";

    public HealthApi(RequestSpecification spec) {
        super(spec);
    }

    public Response ping() {
        return request().get(PING);
    }
}
