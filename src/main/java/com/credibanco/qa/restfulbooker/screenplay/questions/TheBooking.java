package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import io.restassured.response.Response;

import java.util.Optional;

/**
 * {@code GET /booking/{id}} executed when asked. The response is recorded (for status, schema, headers or SLA
 * checks); the booking is returned only when it exists.
 */
public final class TheBooking {

    private TheBooking() {
    }

    public static Question<Optional<Booking>> withId(long bookingId) {
        return Question.about("the booking " + bookingId, actor -> {
            Response response = CallTheApi.as(actor).publicBookingApiFor(actor).getById(bookingId);
            actor.recordResponse(response);
            return response.statusCode() == 200 ? Optional.of(response.as(Booking.class)) : Optional.empty();
        });
    }
}
