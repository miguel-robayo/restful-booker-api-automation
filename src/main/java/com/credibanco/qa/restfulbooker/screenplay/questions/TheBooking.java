package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import com.credibanco.qa.restfulbooker.screenplay.core.UnexpectedResponseException;
import io.restassured.response.Response;

import java.util.Optional;

/**
 * {@code GET /booking/{id}} executed when asked. The response is also recorded as the actor's last response.
 * {@link #withId} answers with the booking, only when it exists; {@link #responseFor} answers with the whole
 * response, as evidence for status, schema, header or SLA checks, without depending on what was read before.
 */
public final class TheBooking {

    private static final int FOUND = 200;
    private static final int NOT_FOUND = 404;

    private TheBooking() {
    }

    public static Question<Optional<Booking>> withId(long bookingId) {
        return Question.about("the booking " + bookingId, actor -> {
            Response response = read(actor, bookingId);
            return switch (response.statusCode()) {
                case FOUND -> Optional.of(response.as(Booking.class));
                case NOT_FOUND -> Optional.empty();
                default -> throw new UnexpectedResponseException("GET /booking/" + bookingId, response,
                        "HTTP " + FOUND + " (found) or " + NOT_FOUND + " (does not exist)");
            };
        });
    }

    public static Question<Response> responseFor(long bookingId) {
        return Question.about("reading booking " + bookingId, actor -> read(actor, bookingId));
    }

    private static Response read(Actor actor, long bookingId) {
        Response response = CallTheApi.as(actor).publicBookingApiFor(actor).getById(bookingId);
        actor.recordResponse(response);
        return response;
    }
}
