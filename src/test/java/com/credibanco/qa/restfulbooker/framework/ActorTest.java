package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.screenplay.abilities.TrackCreatedBookings;
import com.credibanco.qa.restfulbooker.screenplay.abilities.UseAnAuthToken;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Performable;
import com.credibanco.qa.restfulbooker.screenplay.core.PreconditionFailedException;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screenplay core behavior with in-memory actions. No network.
 */
@DisplayName("Screenplay actor")
class ActorTest {

    @Test
    @DisplayName("performs actions in order")
    void performsActionsInOrder() {
        List<String> performed = new ArrayList<>();
        Actor actor = Actor.named("Guest");

        actor.attemptsTo(recording("first", performed), recording("second", performed));

        assertEquals(List.of("first", "second"), performed);
    }

    @Test
    @DisplayName("answers questions")
    void answersQuestions() {
        Actor actor = Actor.named("Guest");

        assertEquals("Guest", actor.asksFor(Question.about("the actor name", Actor::name)));
    }

    @Test
    @DisplayName("replaces an ability of the same type, e.g. a fresh token")
    void replacesAbilityOfSameType() {
        Actor actor = Actor.named("Guest").can(UseAnAuthToken.withToken("old")).can(UseAnAuthToken.withToken("new"));

        assertEquals("new", UseAnAuthToken.as(actor).token());
    }

    @Test
    @DisplayName("explains a missing ability")
    void explainsMissingAbility() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> TrackCreatedBookings.as(Actor.named("Guest")));

        assertTrue(error.getMessage().contains("TrackCreatedBookings"));
    }

    @Test
    @DisplayName("remembers values and explains a missing one")
    void remembersValues() {
        Actor actor = Actor.named("Guest");
        actor.remember("booking", 42);

        assertAll(
                () -> assertEquals(42, (int) actor.recall("booking")),
                () -> assertThrows(NoSuchElementException.class, () -> actor.recall("missing")));
    }

    @Test
    @DisplayName("reports a failing precondition with the PRECONDITION prefix (D-09)")
    void wrapsPreconditionFailures() {
        Actor actor = Actor.named("Guest");

        PreconditionFailedException error = assertThrows(PreconditionFailedException.class,
                () -> actor.wasAbleTo(failingWith(new IllegalStateException("boom"))));

        assertTrue(error.getMessage().startsWith("PRECONDITION: "), error.getMessage());
    }

    @Test
    @DisplayName("reports a precondition that ran but did not succeed")
    void reportsUnsuccessfulPrecondition() {
        Performable unsuccessful = new Performable() {
            @Override
            public void performAs(Actor actor) {
            }

            @Override
            public boolean succeededFor(Actor actor) {
                return false;
            }
        };

        assertThrows(PreconditionFailedException.class, () -> Actor.named("Guest").wasAbleTo(unsuccessful));
    }

    @Test
    @DisplayName("lets network failures through unchanged, so they are classified as Infrastructure")
    void keepsNetworkFailures() {
        UncheckedIOException network = new UncheckedIOException(new ConnectException("Connection refused"));

        UncheckedIOException error = assertThrows(UncheckedIOException.class,
                () -> Actor.named("Guest").wasAbleTo(failingWith(network)));

        assertSame(network, error);
    }

    @Test
    @DisplayName("does not wrap failures of the action under test")
    void doesNotWrapActionFailures() {
        IllegalStateException failure = new IllegalStateException("boom");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> Actor.named("Guest").attemptsTo(failingWith(failure)));

        assertSame(failure, error);
    }

    @Test
    @DisplayName("never exposes the token in descriptions")
    void hidesToken() {
        assertFalse(UseAnAuthToken.withToken("token-x").toString().contains("token-x"));
    }

    @Test
    @DisplayName("tracks created bookings and returns an immutable copy")
    void tracksCreatedBookings() {
        TrackCreatedBookings tracker = TrackCreatedBookings.forThisScenario();
        tracker.track(7, "Lamar-abc");

        assertAll(
                () -> assertEquals(List.of(new TrackCreatedBookings.CreatedBooking(7, "Lamar-abc")), tracker.created()),
                () -> assertThrows(UnsupportedOperationException.class, () -> tracker.created().clear()));
    }

    private static Performable recording(String name, List<String> performed) {
        return actor -> performed.add(name);
    }

    private static Performable failingWith(RuntimeException failure) {
        return actor -> {
            throw failure;
        };
    }
}
