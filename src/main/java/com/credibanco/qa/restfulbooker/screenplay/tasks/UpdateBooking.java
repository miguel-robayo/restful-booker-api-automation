package com.credibanco.qa.restfulbooker.screenplay.tasks;

import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Performable;

/**
 * {@code PUT /booking/{id}} with the actor's token if it holds one; without it for negative scenarios.
 */
public final class UpdateBooking implements Performable {

    private final long bookingId;
    private final Object payload;

    private UpdateBooking(long bookingId, Object payload) {
        this.bookingId = bookingId;
        this.payload = payload;
    }

    public static UpdateBooking of(long bookingId, Object payload) {
        return new UpdateBooking(bookingId, payload);
    }

    @Override
    public void performAs(Actor actor) {
        actor.recordResponse(CallTheApi.as(actor).bookingApiFor(actor).update(bookingId, payload));
    }

    @Override
    public String toString() {
        return "update booking " + bookingId;
    }
}
