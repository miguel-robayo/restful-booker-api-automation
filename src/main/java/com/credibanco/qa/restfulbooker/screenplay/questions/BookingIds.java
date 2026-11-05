package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

/**
 * {@code GET /booking} with filters (RC-03), executed when asked. IDs are extracted with GPath, without loops.
 */
public final class BookingIds {

    private static final String FIRST_NAME = "firstname";
    private static final String IDS = "bookingid";

    private BookingIds() {
    }

    public static Question<List<Integer>> withFirstName(String firstName) {
        return matching(Map.of(FIRST_NAME, firstName));
    }

    public static Question<List<Integer>> matching(Map<String, ?> filters) {
        return Question.about("the booking ids matching " + filters, actor -> {
            Response response = CallTheApi.as(actor).publicBookingApiFor(actor).findIds(filters);
            actor.recordResponse(response);
            return response.statusCode() == 200 ? response.jsonPath().getList(IDS, Integer.class) : List.of();
        });
    }
}
