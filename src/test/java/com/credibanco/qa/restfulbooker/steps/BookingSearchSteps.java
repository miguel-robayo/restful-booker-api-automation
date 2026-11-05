package com.credibanco.qa.restfulbooker.steps;

import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.context.PostActionObservations;
import com.credibanco.qa.restfulbooker.context.ScenarioContext;
import com.credibanco.qa.restfulbooker.data.BookingDataFactory;
import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;
import com.credibanco.qa.restfulbooker.screenplay.questions.BookingDetails;
import com.credibanco.qa.restfulbooker.screenplay.questions.BookingIds;
import com.credibanco.qa.restfulbooker.screenplay.questions.BookingsPricedAbove;
import com.credibanco.qa.restfulbooker.screenplay.questions.HighestBookingId;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheBooking;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheLastResponse;
import com.credibanco.qa.restfulbooker.screenplay.tasks.CreateBooking;
import com.credibanco.qa.restfulbooker.support.Expect;
import com.credibanco.qa.restfulbooker.support.HttpStatus;
import com.credibanco.qa.restfulbooker.support.Preconditions;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.CREATED_BOOKINGS_BY_ID;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.FOUND_BOOKING_DETAILS;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.FOUND_BOOKING_IDS;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.GUEST_UNIQUE_NAME;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.IDENTIFIED_BOOKING;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.IDENTIFIED_BOOKING_ID;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.NONEXISTENT_BOOKING_ID;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.NONEXISTENT_BOOKING_RESPONSE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.SELECTED_BOOKING_IDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Searching Miguel's bookings by value and reading a nonexistent booking.
 *
 * <p>Technical limitation (H-01, C-01): the API search by name returns ids only, without the price. The business
 * search "reservas de valor superior al de referencia" is therefore the search by Miguel's unique name (the action)
 * followed by post-action observations: the real details of each found booking, the GPath selection over them
 * (D-01, threshold excluded) and the details of one identified booking. No data is simulated.
 */
public class BookingSearchSteps {

    private static final int BOOKINGS_AROUND_REFERENCE_VALUE = 4;

    private final Actor miguel;
    private final BookingDataFactory testData;
    private final EnvironmentConfig config;
    private final PostActionObservations observations;

    public BookingSearchSteps(ScenarioContext context) {
        this.miguel = context.miguel();
        this.testData = context.testData();
        this.config = context.config();
        this.observations = context.observations();
    }

    // ---------- Dado ----------

    /** Prices: reference value - 1, reference value, reference value + 1 and reference value + 500. */
    @Dado("que Miguel tiene cuatro reservas a su nombre con valores alrededor del valor de referencia")
    public void registerBookingsAroundReferenceValue() {
        int referenceValue = config.queryPriceThreshold();
        String guestUniqueName = testData.uniqueFirstName();
        Map<Integer, Booking> createdBookingsById = new LinkedHashMap<>();
        List.of(referenceValue - 1, referenceValue, referenceValue + 1, referenceValue + 500).forEach(price -> {
            Booking booking = testData.aBookingWithFirstNameAndPrice(guestUniqueName, price);
            miguel.wasAbleTo(CreateBooking.with(booking));
            createdBookingsById.put(miguel.asksFor(TheLastResponse.bodyAs(BookingResponse.class)).getBookingId(), booking);
        });
        miguel.remember(GUEST_UNIQUE_NAME, guestUniqueName);
        miguel.remember(CREATED_BOOKINGS_BY_ID, createdBookingsById);
    }

    /**
     * Safe nonexistent number: the configured candidate (9999999 per the test document) must be above the highest
     * existing id. Ids are sequential, so nobody can reach it before the query; otherwise: precondition failure.
     */
    @Dado("que Miguel tiene un número de reserva que no corresponde a ninguna reserva existente")
    public void chooseNonexistentBookingNumber() {
        long candidateBookingId = config.notFoundBookingId();
        int highestExistingId = Preconditions.present(miguel.asksFor(HighestBookingId.current()),
                "the booking list must be readable to verify the candidate number");
        AllureReporter.attach("Nonexistent booking number check", "Candidate: " + candidateBookingId
                + "\nHighest existing id: " + highestExistingId + "\nRule: candidate > highest existing id");
        Preconditions.require(candidateBookingId > highestExistingId,
                "candidate " + candidateBookingId + " is not above the highest existing id " + highestExistingId);
        miguel.remember(NONEXISTENT_BOOKING_ID, candidateBookingId);
    }

    // ---------- Cuando (one action; evidence reads are declared as post-action observations) ----------

    @Cuando("busca entre sus reservas las de valor superior al de referencia")
    public void searchOwnBookingsAboveReferenceValue() {
        miguel.remember(FOUND_BOOKING_IDS, miguel.asksFor(BookingIds.withFirstName(miguel.recall(GUEST_UNIQUE_NAME))));
        observations.after("details of every found booking", detailsOfFoundBookings(), FOUND_BOOKING_DETAILS);
        observations.after("selection above the reference value", selectionAboveReferenceValue(), SELECTED_BOOKING_IDS);
        observations.after("first identified booking", firstIdentifiedBookingId(), IDENTIFIED_BOOKING_ID);
        observations.after("details of the identified booking", detailsOfIdentifiedBooking(), IDENTIFIED_BOOKING);
    }

    @Cuando("consulta esa reserva")
    public void retrieveNonexistentBooking() {
        miguel.asksFor(TheBooking.withId(miguel.<Long>recall(NONEXISTENT_BOOKING_ID)));
        miguel.remember(NONEXISTENT_BOOKING_RESPONSE, miguel.asksFor(TheLastResponse.raw()));
    }

    // ---------- Entonces (no HTTP: stored evidence only) ----------

    @Entonces("la búsqueda por su nombre encuentra exactamente sus cuatro reservas")
    public void verifySearchFindsExactlyOwnBookings() {
        Set<Integer> createdBookingIds = createdBookingsById().keySet();
        List<Integer> foundBookingIds = miguel.recall(FOUND_BOOKING_IDS);
        assertAll(
                () -> assertEquals(BOOKINGS_AROUND_REFERENCE_VALUE, foundBookingIds.size(), "found ids: " + foundBookingIds),
                () -> assertEquals(createdBookingIds, new HashSet<>(foundBookingIds), "the search must find exactly Miguel's bookings"));
    }

    @Entonces("solo se identifican las reservas que superan el valor de referencia")
    public void verifyOnlyBookingsAboveReferenceValueIdentified() {
        int referenceValue = config.queryPriceThreshold();
        Set<Integer> expectedBookingIds = createdBookingsById().entrySet().stream()
                .filter(entry -> entry.getValue().getTotalPrice() > referenceValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        List<Integer> selectedBookingIds = miguel.recall(SELECTED_BOOKING_IDS);
        assertAll(
                () -> assertEquals(2, selectedBookingIds.size(), "selected ids: " + selectedBookingIds),
                () -> assertEquals(expectedBookingIds, new HashSet<>(selectedBookingIds),
                        "only reference value + 1 and + 500 must be selected (reference value excluded)"));
    }

    @Entonces("el detalle de la reserva identificada coincide con el registrado")
    public void verifyIdentifiedBookingMatchesRegistered() {
        Optional<Integer> identifiedBookingId = miguel.recall(IDENTIFIED_BOOKING_ID);
        Optional<Booking> identifiedBooking = miguel.recall(IDENTIFIED_BOOKING);
        assertTrue(identifiedBookingId.isPresent(), "no booking was identified above the reference value");
        Booking registeredBooking = createdBookingsById().get(identifiedBookingId.get());
        assertAll(
                () -> assertEquals(Optional.of(registeredBooking), identifiedBooking, "details must map to the registered booking"),
                () -> assertTrue(identifiedBooking.map(b -> b.getTotalPrice() > config.queryPriceThreshold()).orElse(false),
                        "the identified booking must be above the reference value"));
    }

    @Entonces("el sistema informa que la reserva no existe")
    public void verifyBookingReportedAsNonexistent() {
        Expect.status(miguel.recall(NONEXISTENT_BOOKING_RESPONSE), HttpStatus.NOT_FOUND, "GET /booking/{nonexistent id}");
    }

    // ---------- post-action observation questions ----------

    private static Question<List<BookingResponse>> detailsOfFoundBookings() {
        return Question.about("details of the found bookings",
                actor -> actor.asksFor(BookingDetails.of(actor.recall(FOUND_BOOKING_IDS))));
    }

    private Question<List<Integer>> selectionAboveReferenceValue() {
        int referenceValue = config.queryPriceThreshold();
        return Question.about("GPath selection above " + referenceValue,
                actor -> actor.asksFor(BookingsPricedAbove.threshold(referenceValue, actor.recall(FOUND_BOOKING_DETAILS))));
    }

    private static Question<Optional<Integer>> firstIdentifiedBookingId() {
        return Question.about("first identified booking id",
                actor -> actor.<List<Integer>>recall(SELECTED_BOOKING_IDS).stream().findFirst());
    }

    private static Question<Optional<Booking>> detailsOfIdentifiedBooking() {
        return Question.about("details of the identified booking",
                actor -> actor.<Optional<Integer>>recall(IDENTIFIED_BOOKING_ID).flatMap(id -> actor.asksFor(TheBooking.withId(id))));
    }

    private Map<Integer, Booking> createdBookingsById() {
        return miguel.recall(CREATED_BOOKINGS_BY_ID);
    }
}
