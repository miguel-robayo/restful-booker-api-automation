package com.credibanco.qa.restfulbooker.config;

import java.util.Locale;

/**
 * Configuration keys and their environment-variable names ({@code base.url} -> {@code RB_BASE_URL}).
 */
final class ConfigKeys {

    static final String ENVIRONMENT = "env";
    static final String ENVIRONMENTS = "environments";
    static final String BASE_URL = "base.url";
    static final String USERNAME = "auth.username";
    static final String PASSWORD = "auth.password";
    static final String SLA_BOOKING_GET_MS = "sla.booking.get.ms";
    static final String QUERY_PRICE_THRESHOLD = "query.price.threshold";
    static final String NOT_FOUND_BOOKING_ID = "notfound.booking.id";
    static final String HTTP_TIMEOUT_MS = "http.timeout.ms";

    /** Documented credential variables (D-05), referenced as placeholders in default.properties. */
    static final String USERNAME_VARIABLE = "RB_USERNAME";
    static final String PASSWORD_VARIABLE = "RB_PASSWORD";

    private static final String ENVIRONMENT_VARIABLE_PREFIX = "RB_";

    private ConfigKeys() {
    }

    static String environmentVariableFor(String key) {
        return ENVIRONMENT_VARIABLE_PREFIX + key.replace('.', '_').toUpperCase(Locale.ROOT);
    }
}
