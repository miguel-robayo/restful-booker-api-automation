package com.credibanco.qa.restfulbooker.screenplay.tasks;

import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Performable;

/**
 * {@code DELETE /booking/{id}} with the actor's token if it holds one; without it for negative scenarios.
 */
public final class DeleteBooking implements Performable {

    private final long bookingId;

    private DeleteBooking(long bookingId) {
        this.bookingId = bookingId;
    }

    public static DeleteBooking withId(long bookingId) {
        return new DeleteBooking(bookingId);
    }

    @Override
    public void performAs(Actor actor) {
        actor.recordResponse(CallTheApi.as(actor).bookingApiFor(actor).delete(bookingId));
    }

    @Override
    public String toString() {
        return "delete booking " + bookingId;
    }
}
