package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.config.ConfigResolver;
import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import io.restassured.builder.ResponseBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.ContentType;
import io.restassured.internal.RestAssuredResponseOptionsImpl;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class StubbedApi implements OrderedFilter {

    static final EnvironmentConfig CONFIG =
            new ConfigResolver(Map.of("env", "qa"), Map.of(), ConfigResolver::loadFromClasspath).resolve();

    private final Map<String, Canned> routes = new ConcurrentHashMap<>();

    private record Canned(int status, String contentType, String body) {
    }

    StubbedApi on(String method, String path, int status, String jsonBody) {
        return on(method, path, status, ContentType.JSON.toString(), jsonBody);
    }

    StubbedApi on(String method, String path, int status, String contentType, String body) {
        routes.put(method + " " + path, new Canned(status, contentType, body));
        return this;
    }

    Actor actor(String name) {
        return Actor.named(name).can(CallTheApi.using(new SpecFactory(CONFIG, this)));
    }

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        String route = request.getMethod() + " " + URI.create(request.getURI()).getPath();
        Canned canned = routes.get(route);
        if (canned == null) {
            throw new AssertionError("Unexpected request in a framework test: " + route);
        }
        return response(canned.status(), canned.contentType(), canned.body(), request.getConfig());
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    static Response response(int status, String contentType, String body) {
        return response(status, contentType, body, RestAssuredConfig.config());
    }

    static Response response(int status, String contentType, String body, Map<String, String> headers) {
        ResponseBuilder builder = new ResponseBuilder().setStatusCode(status).setStatusLine("HTTP/1.1 " + status)
                .setContentType(contentType).setBody(body);
        headers.forEach(builder::setHeader);
        Response response = builder.build();
        ((RestAssuredResponseOptionsImpl<?>) response).setConfig(RestAssuredConfig.config());
        return response;
    }

    private static Response response(int status, String contentType, String body, RestAssuredConfig config) {
        Response response = new ResponseBuilder().setStatusCode(status).setStatusLine("HTTP/1.1 " + status)
                .setContentType(contentType).setBody(body).build();
        ((RestAssuredResponseOptionsImpl<?>) response).setConfig(config);
        return response;
    }
}
