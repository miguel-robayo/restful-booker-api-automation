package com.credibanco.qa.restfulbooker.support;

import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.abilities.TrackCreatedBookings.CreatedBooking;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheBooking;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheLastResponse;
import com.credibanco.qa.restfulbooker.screenplay.tasks.Authenticate;
import com.credibanco.qa.restfulbooker.screenplay.tasks.DeleteBooking;
import com.credibanco.qa.restfulbooker.support.CleanupResult.Outcome;
import com.credibanco.qa.restfulbooker.support.CleanupResult.Status;

import java.util.List;
import java.util.Optional;

/**
 * Deletes the bookings a scenario created, with its own authenticated actor, and only after checking they are still
 * its own (D-12): the API resets its data and reuses ids. Never throws: what cannot be deleted is reported as pending.
 */
public final class BookingCleaner {

    private static final int DELETED = 201;
    private static final int NOT_FOUND = 404;

    private final SpecFactory specs;

    public BookingCleaner(SpecFactory specs) {
        this.specs = specs;
    }

    public CleanupResult clean(List<CreatedBooking> created) {
        if (created.isEmpty()) {
            return CleanupResult.nothingToClean();
        }
        Actor cleaner = Actor.named("Cleaner").can(CallTheApi.using(specs));
        try {
            cleaner.wasAbleTo(Authenticate.withConfiguredCredentials());
        } catch (RuntimeException authenticationFailed) {
            return new CleanupResult(created.stream()
                    .map(booking -> pending(booking, "cleanup authentication failed: " + authenticationFailed.getMessage()))
                    .toList());
        }
        return new CleanupResult(created.stream().map(booking -> cleanOne(cleaner, booking)).toList());
    }

    private static Outcome cleanOne(Actor cleaner, CreatedBooking booking) {
        try {
            Optional<Booking> current = cleaner.asksFor(TheBooking.withId(booking.bookingId()));
            if (current.isEmpty()) {
                int status = cleaner.asksFor(TheLastResponse.statusCode());
                return status == NOT_FOUND
                        ? outcome(booking, Status.ALREADY_GONE, "already deleted or removed by an API reset")
                        : pending(booking, "ownership check answered HTTP " + status);
            }
            if (!booking.firstName().equals(current.get().getFirstName())) {
                return outcome(booking, Status.NOT_OURS, "id reused by another booking after an API reset; not deleted");
            }
            cleaner.attemptsTo(DeleteBooking.withId(booking.bookingId()));
            int status = cleaner.asksFor(TheLastResponse.statusCode());
            return status == DELETED
                    ? outcome(booking, Status.DELETED, "deleted")
                    : pending(booking, "DELETE answered HTTP " + status);
        } catch (RuntimeException failure) {
            return pending(booking, failure.getMessage());
        }
    }

    private static Outcome pending(CreatedBooking booking, String reason) {
        return outcome(booking, Status.PENDING, reason);
    }

    private static Outcome outcome(CreatedBooking booking, Status status, String detail) {
        return new Outcome(booking.bookingId(), booking.firstName(), status, detail);
    }
}
