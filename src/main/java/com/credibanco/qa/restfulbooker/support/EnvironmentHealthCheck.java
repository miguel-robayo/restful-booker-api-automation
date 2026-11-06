package com.credibanco.qa.restfulbooker.support;

import com.credibanco.qa.restfulbooker.api.HealthApi;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import io.restassured.response.Response;

/**
 * Verifies the API answers {@code GET /ping} with 201 (RC-07) before any scenario runs.
 */
public final class EnvironmentHealthCheck {

    private static final int HEALTHY = 201;

    private EnvironmentHealthCheck() {
    }

    public static void verify(SpecFactory specs) {
        Response response;
        try {
            response = new HealthApi(specs.base()).ping();
        } catch (RuntimeException unreachable) {
            throw new InfrastructureException("GET /ping failed: " + unreachable.getMessage(), unreachable);
        }
        if (response.statusCode() != HEALTHY) {
            throw new InfrastructureException("GET /ping answered " + response.statusCode() + " instead of " + HEALTHY);
        }
    }
}
