package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.http.JsonMapper;
import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.path.json.JsonPath;

import java.util.List;

/**
 * Selects, with a Groovy path expression of Rest Assured's {@link JsonPath}, the ids of the bookings whose price is
 * strictly greater than a threshold (D-01). Derived from details already obtained: no HTTP call.
 */
public final class BookingsPricedAbove {

    private static final String SELECTION = "findAll { it.booking.totalprice > %d }.bookingid";

    private BookingsPricedAbove() {
    }

    public static Question<List<Integer>> threshold(int threshold, List<BookingResponse> details) {
        String expression = SELECTION.formatted(threshold);
        return Question.about("the bookings selected with GPath '" + expression + "'",
                actor -> JsonPath.from(asJson(details)).getList(expression, Integer.class));
    }

    private static String asJson(List<BookingResponse> details) {
        try {
            return JsonMapper.instance().writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize booking details", e);
        }
    }
}
