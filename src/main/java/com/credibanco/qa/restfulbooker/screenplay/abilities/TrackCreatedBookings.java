package com.credibanco.qa.restfulbooker.screenplay.abilities;

import com.credibanco.qa.restfulbooker.screenplay.core.Ability;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;

import java.util.ArrayList;
import java.util.List;

/**
 * Records every booking the actor created, with its first name as ownership marker, so the cleanup can delete only
 * its own bookings (D-12). Owned by one actor; not shared.
 */
public final class TrackCreatedBookings implements Ability {

    private final List<CreatedBooking> created = new ArrayList<>();

    public static TrackCreatedBookings forThisScenario() {
        return new TrackCreatedBookings();
    }

    public static TrackCreatedBookings as(Actor actor) {
        return actor.abilityTo(TrackCreatedBookings.class);
    }

    public void track(int bookingId, String firstName) {
        created.add(new CreatedBooking(bookingId, firstName));
    }

    public List<CreatedBooking> created() {
        return List.copyOf(created);
    }

    /** A booking created by the actor and the first name that proves it is still the actor's own. */
    public record CreatedBooking(int bookingId, String firstName) {
    }
}
