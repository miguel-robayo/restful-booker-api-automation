package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import io.restassured.response.Response;

/**
 * Reads the response the actor already holds. No HTTP call, so these are the questions "Entonces" steps use.
 */
public final class TheLastResponse {

    private TheLastResponse() {
    }

    public static Question<Response> raw() {
        return Question.about("the last response", TheLastResponse::of);
    }

    public static Question<Integer> statusCode() {
        return Question.about("the last response status", actor -> of(actor).statusCode());
    }

    public static <T> Question<T> bodyAs(Class<T> type) {
        return Question.about("the last response body as " + type.getSimpleName(), actor -> of(actor).as(type));
    }

    private static Response of(Actor actor) {
        return actor.lastResponse()
                .orElseThrow(() -> new IllegalStateException(actor + " has not received any response yet"));
    }
}
