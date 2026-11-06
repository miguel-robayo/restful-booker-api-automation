package com.credibanco.qa.restfulbooker.support;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;

import java.util.concurrent.TimeUnit;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.lessThan;

/**
 * Reusable response expectations (RNF-08), applied with {@code response.then().spec(...)} on stored responses.
 * A failed validation triggers Rest Assured's masked conditional log.
 */
public final class ResponseSpecs {

    private static final String BOOKING_SCHEMA = "schemas/booking.json";
    private static final String CONTENT_TYPE = "Content-Type";

    private ResponseSpecs() {
    }

    /** Contract of a booking (RC-01, D-15). */
    public static ResponseSpecification bookingContract() {
        return new ResponseSpecBuilder().expectBody(matchesJsonSchemaInClasspath(BOOKING_SCHEMA)).build();
    }

    /** Response time strictly below the threshold, i.e. {@code time(lessThan(thresholdMs), MILLISECONDS)}. */
    public static ResponseSpecification responseTimeBelow(long thresholdMs) {
        return new ResponseSpecBuilder().expectResponseTime(lessThan(thresholdMs), TimeUnit.MILLISECONDS).build();
    }

    /** Exact {@code Content-Type} value. */
    public static ResponseSpecification contentType(String expected) {
        return new ResponseSpecBuilder().expectHeader(CONTENT_TYPE, expected).build();
    }
}
