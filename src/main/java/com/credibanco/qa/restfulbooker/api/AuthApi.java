package com.credibanco.qa.restfulbooker.api;

import com.credibanco.qa.restfulbooker.model.AuthRequest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * {@code /auth} endpoint.
 */
public final class AuthApi extends BaseApi {

    private static final String AUTH = "/auth";

    public AuthApi(RequestSpecification spec) {
        super(spec);
    }

    public Response createToken(AuthRequest credentials) {
        return request().body(credentials).post(AUTH);
    }
}
