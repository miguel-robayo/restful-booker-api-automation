package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import com.credibanco.qa.restfulbooker.screenplay.core.UnexpectedResponseException;
import io.restassured.response.Response;

import java.util.Map;

/**
 * Highest booking id currently in the API, via {@code GET /booking} and the GPath {@code bookingid.max()}.
 */
public final class HighestBookingId {

    private static final String MAX_ID = "bookingid.max()";
    private static final int OK = 200;

    private HighestBookingId() {
    }

    public static Question<Integer> current() {
        return Question.about("the highest existing booking id", actor -> {
            Response response = CallTheApi.as(actor).publicBookingApiFor(actor).findIds(Map.of());
            actor.recordResponse(response);
            if (response.statusCode() != OK) {
                throw new UnexpectedResponseException("GET /booking", response, "HTTP " + OK);
            }
            Integer max = response.jsonPath().get(MAX_ID);
            return max == null ? 0 : max;
        });
    }
}
