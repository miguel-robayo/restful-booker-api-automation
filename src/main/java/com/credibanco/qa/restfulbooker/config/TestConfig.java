package com.credibanco.qa.restfulbooker.config;

import java.net.URI;

/**
 * Immutable configuration of a test run. Credentials are validated on first use, so technical tests that never
 * call the API do not need them.
 */
public final class TestConfig {

    private static volatile TestConfig current;

    private final String environment;
    private final URI baseUrl;
    private final String username;
    private final String password;
    private final long slaBookingGetMs;
    private final int queryPriceThreshold;
    private final long notFoundBookingId;
    private final int httpTimeoutMs;

    TestConfig(String environment, URI baseUrl, String username, String password, long slaBookingGetMs,
               int queryPriceThreshold, long notFoundBookingId, int httpTimeoutMs) {
        this.environment = environment;
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        this.slaBookingGetMs = slaBookingGetMs;
        this.queryPriceThreshold = queryPriceThreshold;
        this.notFoundBookingId = notFoundBookingId;
        this.httpTimeoutMs = httpTimeoutMs;
    }

    /**
     * Configuration resolved from the running JVM (system properties, environment variables and classpath files).
     * Resolved once; a failed resolution is retried on the next call so the error is always the real cause.
     */
    public static TestConfig current() {
        TestConfig config = current;
        if (config == null) {
            synchronized (TestConfig.class) {
                if (current == null) {
                    current = ConfigResolver.fromRuntime().resolve();
                }
                config = current;
            }
        }
        return config;
    }

    public String environment() {
        return environment;
    }

    public URI baseUrl() {
        return baseUrl;
    }

    public String username() {
        return requireCredential(username, ConfigKeys.USERNAME, ConfigKeys.USERNAME_VARIABLE);
    }

    public String password() {
        return requireCredential(password, ConfigKeys.PASSWORD, ConfigKeys.PASSWORD_VARIABLE);
    }

    public long slaBookingGetMs() {
        return slaBookingGetMs;
    }

    public int queryPriceThreshold() {
        return queryPriceThreshold;
    }

    public long notFoundBookingId() {
        return notFoundBookingId;
    }

    public int httpTimeoutMs() {
        return httpTimeoutMs;
    }

    private static String requireCredential(String value, String key, String variable) {
        if (value == null) {
            throw new ConfigurationException("Missing credential '" + key + "'. Set the environment variable "
                    + variable + " or pass -D" + key + "=<value>.");
        }
        return value;
    }

    @Override
    public String toString() {
        return "TestConfig[environment=" + environment
                + ", baseUrl=" + baseUrl
                + ", username=" + (username == null ? "<missing>" : "<set>")
                + ", password=" + (password == null ? "<missing>" : "<set>")
                + ", slaBookingGetMs=" + slaBookingGetMs
                + ", queryPriceThreshold=" + queryPriceThreshold
                + ", notFoundBookingId=" + notFoundBookingId
                + ", httpTimeoutMs=" + httpTimeoutMs + "]";
    }
}
