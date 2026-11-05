package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import io.restassured.response.Response;

import java.util.Map;
import java.util.Optional;

/**
 * Highest booking id currently in the API, via {@code GET /booking} and the GPath {@code bookingid.max()}.
 * Empty when the list cannot be read.
 */
public final class HighestBookingId {

    private static final String MAX_ID = "bookingid.max()";

    private HighestBookingId() {
    }

    public static Question<Optional<Integer>> current() {
        return Question.about("the highest existing booking id", actor -> {
            Response response = CallTheApi.as(actor).publicBookingApiFor(actor).findIds(Map.of());
            actor.recordResponse(response);
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            Integer max = response.jsonPath().get(MAX_ID);
            return Optional.of(max == null ? 0 : max);
        });
    }
}
