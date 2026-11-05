package com.credibanco.qa.restfulbooker.screenplay.core;

/**
 * A business action performed by an {@link Actor} (a write, ADR-03). Its {@code toString} is the step name shown in
 * the report, so it must never contain secrets.
 */
public interface Performable {

    void performAs(Actor actor);

    /**
     * Whether the action achieved its goal, checked only when used as a precondition ({@link Actor#wasAbleTo}).
     * By default: the last response is 2xx.
     */
    default boolean succeededFor(Actor actor) {
        return actor.lastResponse().map(response -> response.statusCode() / 100 == 2).orElse(true);
    }
}
