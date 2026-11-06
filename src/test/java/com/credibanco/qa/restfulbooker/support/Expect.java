package com.credibanco.qa.restfulbooker.support;

import com.credibanco.qa.restfulbooker.http.SensitiveDataMasker;
import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Reusable assertions on results already received. Used only by "Entonces" steps; never makes HTTP calls.
 */
public final class Expect {

    private static final int MAX_BODY_IN_MESSAGE = 300;

    private Expect() {
    }

    public static void status(Response response, int expected, String operation) {
        assertEquals(expected, response.statusCode(),
                operation + " returned an unexpected status. Body: " + maskedBody(response));
    }

    /**
     * SLA as a non-functional requirement (D-11). The measured time, threshold and scope are attached to Allure
     * <b>before</b> comparing, so they are captured even when the threshold is exceeded. The failure message starts
     * with {@code SLA:} so Allure classifies it as Performance (SLA), not as a functional defect. The comparison itself
     * is Rest Assured's {@code time(lessThan(...))} via {@link ResponseSpecs#responseTimeBelow(long)}.
     */
    public static void sla(Response response, long thresholdMs, String scope) {
        long measuredMs = response.time();
        AllureReporter.attach("SLA measurement", """
                Scope: %s
                Measured: %d ms (client side, Rest Assured: from sending the request to receiving the full response)
                Threshold: strictly less than %d ms (sla.booking.get.ms)
                Result: %s
                """.formatted(scope, measuredMs, thresholdMs, slaResult(measuredMs, thresholdMs)));
        try {
            response.then().spec(ResponseSpecs.responseTimeBelow(thresholdMs));
        } catch (AssertionError e) {
            throw new AssertionError("SLA: " + scope + " took " + measuredMs + " ms; required < " + thresholdMs + " ms", e);
        }
    }

    private static String slaResult(long measuredMs, long thresholdMs) {
        if (measuredMs < 0) {
            return "no response time was recorded";
        }
        return measuredMs < thresholdMs ? "within SLA" : "SLA exceeded";
    }

    public static void headerPresent(Response response, String name) {
        String value = response.getHeader(name);
        assertNotNull(value, "Header '" + name + "' is missing");
        assertFalse(value.isBlank(), "Header '" + name + "' is empty");
    }

    private static String maskedBody(Response response) {
        String body = SensitiveDataMasker.maskBody(response.asString());
        return body == null || body.length() <= MAX_BODY_IN_MESSAGE ? body : body.substring(0, MAX_BODY_IN_MESSAGE) + "…";
    }
}
