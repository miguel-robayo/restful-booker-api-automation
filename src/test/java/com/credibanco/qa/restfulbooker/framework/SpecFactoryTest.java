package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.config.ConfigResolver;
import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.http.MaskedAllureFilter;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.http.TokenCookieFilter;
import io.restassured.filter.Filter;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Centralized, isolated request specifications. No network.
 */
@DisplayName("Spec factory")
class SpecFactoryTest {

    private static final EnvironmentConfig CONFIG =
            new ConfigResolver(Map.of("env", "qa"), Map.of(), ConfigResolver::loadFromClasspath).resolve();

    private final SpecFactory specs = new SpecFactory(CONFIG);

    @Test
    @DisplayName("builds the base spec from configuration with JSON headers and the masked Allure filter")
    void buildsBaseSpec() {
        QueryableRequestSpecification base = SpecificationQuerier.query(specs.base());

        assertAll(
                () -> assertEquals("https://restful-booker.herokuapp.com", base.getBaseUri()),
                () -> assertTrue(base.getContentType().startsWith("application/json"), base.getContentType()),
                () -> assertEquals("application/json", base.getHeaders().getValue("Accept")),
                () -> assertEquals(List.of(MaskedAllureFilter.class), filterTypes(base)));
    }

    @Test
    @DisplayName("adds the token cookie filter only to authenticated specs")
    void addsTokenFilterOnlyWhenAuthenticated() {
        List<Class<?>> authenticated = filterTypes(SpecificationQuerier.query(specs.authenticated("token-x")));
        List<Class<?>> base = filterTypes(SpecificationQuerier.query(specs.base()));

        assertAll(
                () -> assertTrue(authenticated.contains(TokenCookieFilter.class)),
                () -> assertTrue(authenticated.contains(MaskedAllureFilter.class)),
                () -> assertEquals(List.of(MaskedAllureFilter.class), base));
    }

    @Test
    @DisplayName("returns a new spec on every call, so specs are never shared between actors")
    void returnsNewSpecEachTime() {
        RequestSpecification first = specs.authenticated("token-a");
        RequestSpecification second = specs.authenticated("token-b");

        assertAll(
                () -> assertNotSame(first, second),
                () -> assertNotSame(specs.base(), specs.base()));
    }

    @Test
    @DisplayName("never exposes the token in the filter's toString")
    void hidesTokenInFilterDescription() {
        assertTrue(!new TokenCookieFilter("token-x").toString().contains("token-x"));
    }

    private static List<Class<?>> filterTypes(QueryableRequestSpecification spec) {
        return spec.getDefinedFilters().stream().<Class<?>>map(Filter::getClass).toList();
    }
}
