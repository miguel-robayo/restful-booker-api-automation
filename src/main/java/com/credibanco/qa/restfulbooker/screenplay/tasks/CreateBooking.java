package com.credibanco.qa.restfulbooker.screenplay.tasks;

import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.abilities.TrackCreatedBookings;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Performable;
import io.restassured.response.Response;

import java.util.List;

/**
 * {@code POST /booking}. A created booking is tracked for cleanup straight from the raw response, before any step
 * asserts on it (D-12).
 */
public final class CreateBooking implements Performable {

    private static final String BOOKING_ID = "bookingid";
    private static final String FIRST_NAME = "booking.firstname";

    private final Object payload;

    private CreateBooking(Object payload) {
        this.payload = payload;
    }

    /** Valid bookings use the model; negative scenarios may pass test-only payloads. */
    public static CreateBooking with(Object payload) {
        return new CreateBooking(payload);
    }

    @Override
    public void performAs(Actor actor) {
        Response response = CallTheApi.as(actor).publicBookingApiFor(actor).create(payload);
        actor.recordResponse(response);
        trackIfCreated(actor, response);
    }

    @Override
    public boolean succeededWith(List<Response> ownResponses) {
        return Performable.super.succeededWith(ownResponses) && createdBookingId(ownResponses.getLast()) != null;
    }

    private static void trackIfCreated(Actor actor, Response response) {
        if (response.statusCode() / 100 != 2 || !actor.hasAbilityTo(TrackCreatedBookings.class)) {
            return;
        }
        Integer bookingId = createdBookingId(response);
        if (bookingId != null) {
            TrackCreatedBookings.as(actor).track(bookingId, response.path(FIRST_NAME));
        }
    }

    private static Integer createdBookingId(Response response) {
        return response.path(BOOKING_ID);
    }

    @Override
    public String toString() {
        return payload instanceof Booking booking
                ? "create a booking for '" + booking.getFirstName() + "'"
                : "create a booking with a " + payload.getClass().getSimpleName();
    }
}
