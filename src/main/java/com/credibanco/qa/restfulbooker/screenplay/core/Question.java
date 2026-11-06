package com.credibanco.qa.restfulbooker.screenplay.core;

import java.util.function.Function;

/**
 * A read answered by an {@link Actor}: an HTTP read executed when asked, or a value derived from what the actor
 * already holds. Never asserts. Its {@code toString} is the step name shown in the report.
 */
@FunctionalInterface
public interface Question<T> {

    T answeredBy(Actor actor);

    /** A question with a readable description for the report. */
    static <T> Question<T> about(String description, Function<Actor, T> answer) {
        return new Question<>() {
            @Override
            public T answeredBy(Actor actor) {
                return answer.apply(actor);
            }

            @Override
            public String toString() {
                return description;
            }
        };
    }
}
