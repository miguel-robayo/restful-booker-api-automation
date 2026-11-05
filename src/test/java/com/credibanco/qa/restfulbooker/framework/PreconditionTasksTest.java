package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.model.AuthRequest;
import com.credibanco.qa.restfulbooker.screenplay.abilities.TrackCreatedBookings;
import com.credibanco.qa.restfulbooker.screenplay.abilities.UseAnAuthToken;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.PreconditionFailedException;
import com.credibanco.qa.restfulbooker.screenplay.tasks.Authenticate;
import com.credibanco.qa.restfulbooker.screenplay.tasks.CreateBooking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tasks as preconditions")
class PreconditionTasksTest {

    private static final AuthRequest CREDENTIALS = AuthRequest.builder().username("user-x").password("secret-x").build();
    private static final String CREATED = """
            {"bookingid":42,"booking":{"firstname":"Lamar-abc","lastname":"Smith","totalprice":321,"depositpaid":true,
            "bookingdates":{"checkin":"2027-01-02","checkout":"2027-01-05"},"additionalneeds":"Breakfast"}}""";

    @Test
    @DisplayName("sign-in succeeds only when its own response issues a token")
    void signInSucceedsWithIssuedToken() {
        Actor actor = new StubbedApi().on("POST", "/auth", 200, "{\"token\":\"abc123\"}").actor("Guest");

        actor.wasAbleTo(Authenticate.with(CREDENTIALS));

        assertEquals("abc123", UseAnAuthToken.as(actor).token());
    }

    @Test
    @DisplayName("sign-in answered 200 with a rejection reason (KD-01) is a precondition failure")
    void signInRejectedIsPreconditionFailure() {
        Actor actor = new StubbedApi().on("POST", "/auth", 200, "{\"reason\":\"Bad credentials\"}").actor("Guest");

        assertThrows(PreconditionFailedException.class, () -> actor.wasAbleTo(Authenticate.with(CREDENTIALS)));
        assertFalse(actor.hasAbilityTo(UseAnAuthToken.class));
    }

    @Test
    @DisplayName("creating a booking succeeds and tracks it when its own response has the booking number")
    void creationSucceedsWithBookingId() {
        Actor actor = new StubbedApi().on("POST", "/booking", 200, CREATED).actor("Guest")
                .can(TrackCreatedBookings.forThisScenario());

        actor.wasAbleTo(CreateBooking.with("{}"));

        assertEquals(List.of(new TrackCreatedBookings.CreatedBooking(42, "Lamar-abc")),
                TrackCreatedBookings.as(actor).created());
    }

    @Test
    @DisplayName("M-04: a 2xx creation without a booking number is a precondition failure")
    void creationWithoutBookingIdIsPreconditionFailure() {
        Actor actor = new StubbedApi().on("POST", "/booking", 200, "{}").actor("Guest");

        PreconditionFailedException error = assertThrows(PreconditionFailedException.class,
                () -> actor.wasAbleTo(CreateBooking.with("{}")));

        assertTrue(error.getMessage().startsWith("PRECONDITION: "), error.getMessage());
    }

    @Test
    @DisplayName("M-04: a 2xx creation with an unreadable body is a precondition failure")
    void creationWithUnreadableBodyIsPreconditionFailure() {
        Actor actor = new StubbedApi().on("POST", "/booking", 200, "text/plain", "OK").actor("Guest")
                .can(TrackCreatedBookings.forThisScenario());

        assertAll(
                () -> assertThrows(PreconditionFailedException.class, () -> actor.wasAbleTo(CreateBooking.with("{}"))),
                () -> assertEquals(List.of(), TrackCreatedBookings.as(actor).created()));
    }
}
