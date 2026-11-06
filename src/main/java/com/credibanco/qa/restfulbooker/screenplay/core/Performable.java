package com.credibanco.qa.restfulbooker.screenplay.core;

import io.restassured.response.Response;

import java.util.List;

/**
 * A business action performed by an {@link Actor} (a write, ADR-03). Its {@code toString} is the step name shown in
 * the report, so it must never contain secrets.
 */
public interface Performable {

    void performAs(Actor actor);

    /**
     * Whether the action achieved its goal, checked only when used as a precondition ({@link Actor#wasAbleTo}).
     */
    default boolean succeededWith(List<Response> ownResponses) {
        return !ownResponses.isEmpty()
                && ownResponses.stream().allMatch(response -> response.statusCode() / 100 == 2);
    }
}
