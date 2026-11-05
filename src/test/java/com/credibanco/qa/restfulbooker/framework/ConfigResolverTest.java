package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.config.ConfigResolver;
import com.credibanco.qa.restfulbooker.config.ConfigurationException;
import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FW-02: configuration precedence and fail-fast validation. No network, no global state.
 */
@DisplayName("FW-02 Configuration resolution")
class ConfigResolverTest {

    private static final String QA_URL = "https://qa.example.test";
    private static final String DEV_URL = "https://dev.example.test";

    @Test
    @DisplayName("defaults to the qa environment and merges default and environment files")
    void defaultsToQaEnvironment() {
        EnvironmentConfig config = resolver(Map.of(), Map.of()).resolve();

        assertAll(
                () -> assertEquals("qa", config.environment()),
                () -> assertEquals(URI.create(QA_URL), config.baseUrl()),
                () -> assertEquals(2000L, config.slaBookingGetMs()),
                () -> assertEquals(500, config.queryPriceThreshold()),
                () -> assertEquals(9999999L, config.notFoundBookingId()),
                () -> assertEquals(10000, config.httpTimeoutMs()));
    }

    @Test
    @DisplayName("selects the environment with -Denv")
    void selectsEnvironmentFromSystemProperty() {
        EnvironmentConfig config = resolver(Map.of("env", "dev"), Map.of()).resolve();

        assertAll(
                () -> assertEquals("dev", config.environment()),
                () -> assertEquals(URI.create(DEV_URL), config.baseUrl()));
    }

    @Test
    @DisplayName("applies precedence -D > RB_ variable > environment file > default file")
    void appliesPrecedence() {
        assertEquals(2000L, resolver(Map.of(), Map.of()).resolve().slaBookingGetMs(), "default file");
        assertEquals(1500L, resolver(Map.of(), Map.of(), Map.of("sla.booking.get.ms", "1500"))
                .resolve().slaBookingGetMs(), "environment file over default file");
        assertEquals(1200L, resolver(Map.of(), Map.of("RB_SLA_BOOKING_GET_MS", "1200"), Map.of("sla.booking.get.ms", "1500"))
                .resolve().slaBookingGetMs(), "RB_ variable over environment file");
        assertEquals(900L, resolver(Map.of("sla.booking.get.ms", "900"), Map.of("RB_SLA_BOOKING_GET_MS", "1200"),
                Map.of("sla.booking.get.ms", "1500")).resolve().slaBookingGetMs(), "-D over RB_ variable");
    }

    @Test
    @DisplayName("resolves credential placeholders from environment variables")
    void resolvesCredentialPlaceholders() {
        EnvironmentConfig config = resolver(Map.of(), Map.of("RB_USERNAME", "user-from-env", "RB_PASSWORD", "secret-from-env"))
                .resolve();

        assertAll(
                () -> assertEquals("user-from-env", config.username()),
                () -> assertEquals("secret-from-env", config.password()));
    }

    @Test
    @DisplayName("fails on first use, not on load, when a credential is missing")
    void failsOnFirstUseOfMissingCredential() {
        EnvironmentConfig config = resolver(Map.of(), Map.of()).resolve();

        ConfigurationException error = assertThrows(ConfigurationException.class, config::username);
        assertAll(
                () -> assertTrue(error.getMessage().contains("auth.username")),
                () -> assertTrue(error.getMessage().contains("RB_USERNAME")));
    }

    @Test
    @DisplayName("never exposes credential values in toString")
    void hidesCredentialsInToString() {
        EnvironmentConfig config = resolver(Map.of(), Map.of("RB_USERNAME", "user-from-env", "RB_PASSWORD", "secret-from-env"))
                .resolve();

        String text = config.toString();
        assertAll(
                () -> assertFalse(text.contains("user-from-env")),
                () -> assertFalse(text.contains("secret-from-env")));
    }

    @Test
    @DisplayName("rejects an unknown environment and lists the valid ones")
    void rejectsUnknownEnvironment() {
        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> resolver(Map.of("env", "prod"), Map.of()).resolve());

        assertAll(
                () -> assertTrue(error.getMessage().contains("prod")),
                () -> assertTrue(error.getMessage().contains("dev, qa")));
    }

    @Test
    @DisplayName("rejects a base URL that is not https")
    void rejectsNonHttpsBaseUrl() {
        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> resolver(Map.of("base.url", "http://insecure.example.test"), Map.of()).resolve());

        assertTrue(error.getMessage().contains("https"));
    }

    @Test
    @DisplayName("rejects a non-numeric value with the offending key")
    void rejectsInvalidNumber() {
        ConfigurationException error = assertThrows(ConfigurationException.class,
                () -> resolver(Map.of("sla.booking.get.ms", "fast"), Map.of()).resolve());

        assertTrue(error.getMessage().contains("sla.booking.get.ms"));
    }

    @Test
    @DisplayName("ships valid dev and qa configuration files")
    void shipsValidConfigurationFiles() {
        for (String environment : new String[]{"dev", "qa"}) {
            EnvironmentConfig config = new ConfigResolver(Map.of("env", environment), Map.of(), ConfigResolver::loadFromClasspath)
                    .resolve();
            assertEquals("https", config.baseUrl().getScheme(), environment);
        }
    }

    private static ConfigResolver resolver(Map<String, String> systemProperties, Map<String, String> environment) {
        return resolver(systemProperties, environment, Map.of());
    }

    private static ConfigResolver resolver(Map<String, String> systemProperties, Map<String, String> environment,
                                           Map<String, String> qaOverrides) {
        Map<String, Properties> files = new HashMap<>();
        files.put("default", properties(Map.of(
                "environments", "dev,qa",
                "auth.username", "${RB_USERNAME}",
                "auth.password", "${RB_PASSWORD}",
                "sla.booking.get.ms", "2000",
                "query.price.threshold", "500",
                "notfound.booking.id", "9999999",
                "http.timeout.ms", "10000")));
        Map<String, String> qa = new HashMap<>(Map.of("base.url", QA_URL));
        qa.putAll(qaOverrides);
        files.put("qa", properties(qa));
        files.put("dev", properties(Map.of("base.url", DEV_URL)));
        return new ConfigResolver(systemProperties, environment, name -> Optional.ofNullable(files.get(name)));
    }

    private static Properties properties(Map<String, String> values) {
        Properties properties = new Properties();
        properties.putAll(values);
        return properties;
    }
}
