package com.credibanco.qa.restfulbooker.steps;

import com.credibanco.qa.restfulbooker.context.PostActionObservations;
import com.credibanco.qa.restfulbooker.context.ScenarioContext;
import com.credibanco.qa.restfulbooker.data.BookingDataFactory;
import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.screenplay.abilities.UseAnAuthToken;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheBooking;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheLastResponse;
import com.credibanco.qa.restfulbooker.screenplay.tasks.CreateBooking;
import com.credibanco.qa.restfulbooker.screenplay.tasks.DeleteBooking;
import com.credibanco.qa.restfulbooker.screenplay.tasks.UpdateBooking;
import com.credibanco.qa.restfulbooker.support.Expect;
import com.credibanco.qa.restfulbooker.support.HttpStatus;
import com.credibanco.qa.restfulbooker.support.Preconditions;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;

import java.util.Optional;

import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.BOOKING_DETAILS_RESPONSE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.BOOKING_READ_AFTER_ACTION;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.BOOKING_REQUEST;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.BOOKING_STATE_AFTER_ACTION;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.CANCELLATION_RESPONSE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.CREATION_RESPONSE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.INITIAL_BOOKING_STATE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.OWN_BOOKING;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.OWN_BOOKING_ID;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.UPDATED_BOOKING_DETAILS;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.UPDATE_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registering, modifying, cancelling and reading Miguel's own booking. Business vocabulary: "cancelar" = the API
 * deletes the booking (there is no "cancelled" state), so the outcome is "deja de estar disponible".
 */
public class BookingSteps {

    private final Actor miguel;
    private final BookingDataFactory testData;
    private final PostActionObservations observations;

    public BookingSteps(ScenarioContext context) {
        this.miguel = context.miguel();
        this.testData = context.testData();
        this.observations = context.observations();
    }

    // ---------- Dado ----------

    @Dado("^(?:que Miguel )?tiene una reserva registrada$")
    public void registerOwnBooking() {
        Booking ownBooking = testData.aValidBooking();
        miguel.wasAbleTo(CreateBooking.with(ownBooking));
        miguel.remember(OWN_BOOKING, ownBooking);
        miguel.remember(OWN_BOOKING_ID, miguel.asksFor(TheLastResponse.bodyAs(BookingResponse.class)).getBookingId());
    }

    @Dado("conoce el estado actual de su reserva")
    public void retrieveInitialBookingState() {
        Optional<Booking> currentBooking = Preconditions.obtained(() -> miguel.asksFor(TheBooking.withId(ownBookingId())),
                "the own booking " + ownBookingId() + " must be readable before the action");
        miguel.remember(INITIAL_BOOKING_STATE, Preconditions.present(currentBooking,
                "the own booking " + ownBookingId() + " must exist before the action"));
    }

    /** Same session as the later cancellation: the token comes from "que Miguel inició sesión". */
    @Dado("que ya modificó esa reserva durante la misma sesión")
    public void modifyBookingWithinSession() {
        Preconditions.require(miguel.hasAbilityTo(UseAnAuthToken.class), "Miguel must have signed in first");
        Booking updatedBookingDetails = newDetailsForOwnBooking();
        miguel.wasAbleTo(UpdateBooking.of(ownBookingId(), updatedBookingDetails));
        miguel.remember(UPDATED_BOOKING_DETAILS, updatedBookingDetails);
    }

    @Dado("que Miguel tiene los datos de una nueva reserva de alojamiento")
    public void prepareBookingRequest() {
        miguel.remember(BOOKING_REQUEST, testData.aValidBooking());
    }

    // ---------- Cuando (one action; evidence reads are declared as post-action observations) ----------

    @Cuando("registra la reserva")
    public void registerBooking() {
        miguel.attemptsTo(CreateBooking.with(miguel.<Booking>recall(BOOKING_REQUEST)));
        miguel.remember(CREATION_RESPONSE, miguel.asksFor(TheLastResponse.raw()));
    }

    @Cuando("modifica los datos de su reserva")
    public void updateBooking() {
        miguel.attemptsTo(ownBookingUpdate());
        recordUpdateOutcome("state of the booking after the modification");
    }

    @Cuando("intenta modificar su reserva")
    public void attemptBookingUpdate() {
        miguel.attemptsTo(ownBookingUpdate());
        recordUpdateOutcome("state of the booking after the attempt");
    }

    @Cuando("cancela su reserva")
    public void cancelBooking() {
        miguel.attemptsTo(DeleteBooking.withId(ownBookingId()));
        miguel.remember(CANCELLATION_RESPONSE, miguel.asksFor(TheLastResponse.raw()));
        observations.after("reading the booking after the cancellation", ownBookingReading(), BOOKING_READ_AFTER_ACTION);
    }

    @Cuando("consulta los detalles de su reserva")
    public void retrieveBookingDetails() {
        miguel.remember(BOOKING_DETAILS_RESPONSE, miguel.asksFor(TheBooking.responseFor(ownBookingId())));
    }

    // ---------- Entonces (no HTTP: stored evidence only) ----------

    @Entonces("la reserva queda confirmada")
    public void verifyBookingConfirmed() {
        Expect.status(miguel.recall(CREATION_RESPONSE), HttpStatus.OK, "POST /booking");
    }

    @Entonces("recibe un número de reserva")
    public void verifyBookingNumberReceived() {
        Integer bookingId = miguel.<Response>recall(CREATION_RESPONSE).as(BookingResponse.class).getBookingId();
        assertTrue(bookingId != null && bookingId > 0, "bookingid must be a positive number, was " + bookingId);
    }

    @Entonces("la reserva registrada contiene exactamente los datos ingresados")
    public void verifyRegisteredBookingMatchesRequest() {
        Booking registeredBooking = miguel.<Response>recall(CREATION_RESPONSE).as(BookingResponse.class).getBooking();
        assertEquals(miguel.<Booking>recall(BOOKING_REQUEST), registeredBooking, "the API must store exactly what was sent");
    }

    @Entonces("la modificación queda confirmada con los nuevos datos")
    public void verifyUpdateConfirmedWithNewDetails() {
        Response updateResponse = miguel.recall(UPDATE_RESPONSE);
        Expect.status(updateResponse, HttpStatus.OK, "PUT /booking/{id}");
        assertEquals(miguel.<Booking>recall(UPDATED_BOOKING_DETAILS), updateResponse.as(Booking.class),
                "the modification response must echo the new details");
    }

    @Entonces("la reserva conserva los cambios realizados")
    public void verifyBookingKeepsChanges() {
        assertEquals(Optional.of(miguel.<Booking>recall(UPDATED_BOOKING_DETAILS)), miguel.recall(BOOKING_STATE_AFTER_ACTION),
                "the stored booking must reflect the modification");
    }

    @Entonces("la cancelación queda confirmada")
    public void verifyCancellationConfirmed() {
        Expect.status(miguel.recall(CANCELLATION_RESPONSE), HttpStatus.CREATED, "DELETE /booking/{id}");
    }

    @Entonces("la reserva deja de estar disponible")
    public void verifyBookingNoLongerAvailable() {
        Expect.status(miguel.recall(BOOKING_READ_AFTER_ACTION), HttpStatus.NOT_FOUND, "GET /booking/{id} after DELETE");
    }

    @Entonces("la modificación es rechazada por falta de autorización")
    public void verifyUpdateRejectedAsUnauthorized() {
        Expect.status(miguel.recall(UPDATE_RESPONSE), HttpStatus.FORBIDDEN, "PUT /booking/{id} without a valid session");
    }

    @Entonces("la reserva conserva su información original")
    public void verifyBookingKeptOriginalInformation() {
        assertEquals(Optional.of(miguel.<Booking>recall(INITIAL_BOOKING_STATE)), miguel.recall(BOOKING_STATE_AFTER_ACTION),
                "a rejected modification must leave the booking unchanged");
    }

    // ---------- helpers ----------

    /** Builds (does not perform) the modification of the own booking with new details, and remembers them. */
    private UpdateBooking ownBookingUpdate() {
        Booking updatedBookingDetails = newDetailsForOwnBooking();
        miguel.remember(UPDATED_BOOKING_DETAILS, updatedBookingDetails);
        return UpdateBooking.of(ownBookingId(), updatedBookingDetails);
    }

    /** Stores the modification response and declares the read of the booking state that follows it (ADR-22). */
    private void recordUpdateOutcome(String observation) {
        miguel.remember(UPDATE_RESPONSE, miguel.asksFor(TheLastResponse.raw()));
        observations.after(observation, ownBookingState(), BOOKING_STATE_AFTER_ACTION);
    }

    /** New valid details that keep the first name, so the cleanup still recognizes the booking as Miguel's. */
    private Booking newDetailsForOwnBooking() {
        return testData.aBookingWithFirstName(miguel.<Booking>recall(OWN_BOOKING).getFirstName());
    }

    private Question<Optional<Booking>> ownBookingState() {
        return TheBooking.withId(ownBookingId());
    }

    private Question<Response> ownBookingReading() {
        return TheBooking.responseFor(ownBookingId());
    }

    private int ownBookingId() {
        return miguel.recall(OWN_BOOKING_ID);
    }
}
