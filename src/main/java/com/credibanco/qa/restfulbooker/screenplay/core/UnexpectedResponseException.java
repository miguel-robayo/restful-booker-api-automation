package com.credibanco.qa.restfulbooker.screenplay.core;

import com.credibanco.qa.restfulbooker.http.ResponseSummary;
import io.restassured.response.Response;

public class UnexpectedResponseException extends RuntimeException {

    private final int statusCode;

    public UnexpectedResponseException(String operation, Response response, String expected) {
        super(operation + " answered " + ResponseSummary.of(response) + "; expected " + expected);
        this.statusCode = response.statusCode();
    }

    public int statusCode() {
        return statusCode;
    }
}
