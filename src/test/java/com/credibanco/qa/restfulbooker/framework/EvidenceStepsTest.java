package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.context.ScenarioContext;
import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.steps.BookingQualitySteps;
import com.credibanco.qa.restfulbooker.steps.BookingSearchSteps;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.BOOKING_DETAILS_RESPONSE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.CREATED_BOOKINGS_BY_ID;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.FOUND_BOOKING_DETAILS;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.SELECTED_BOOKING_IDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Then steps on incomplete evidence")
class EvidenceStepsTest {

    private static final String AGREED_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final Map<String, String> SERVER = Map.of("Server", "Heroku");
    private static final String BOOKING_BODY = """
            {"firstname":"Lamar-abc","lastname":"Smith","totalprice":321,"depositpaid":true,
            "bookingdates":{"checkin":"2027-01-02","checkout":"2027-01-05"},"additionalneeds":"Breakfast"}""";

    @Test
    @DisplayName("H-01: a fast error response does not meet \"available within the agreed time\"")
    void slaRequiresTheBooking() {
        BookingQualitySteps steps = qualityStepsWith(StubbedApi.response(503, AGREED_CONTENT_TYPE, "{}", SERVER));

        assertThrows(AssertionError.class, steps::verifyResponseTimeWithinAgreedMaximum);
    }

    @Test
    @DisplayName("H-01: an error response does not meet \"delivered in the agreed format and encoding\"")
    void formatRequiresTheBooking() {
        BookingQualitySteps steps = qualityStepsWith(StubbedApi.response(500, AGREED_CONTENT_TYPE, "{}", SERVER));

        assertThrows(AssertionError.class, steps::verifyAgreedFormatAndEncoding);
    }

    @Test
    @DisplayName("H-01: an error response does not meet \"identifies the server that issues it\"")
    void serverHeaderRequiresTheBooking() {
        BookingQualitySteps steps = qualityStepsWith(StubbedApi.response(500, AGREED_CONTENT_TYPE, "{}", SERVER));

        assertThrows(AssertionError.class, steps::verifyServerIdentified);
    }

    @Test
    @DisplayName("the quality checks pass on the obtained booking")
    void qualityChecksPassOnTheBooking() {
        BookingQualitySteps steps = qualityStepsWith(StubbedApi.response(200, AGREED_CONTENT_TYPE, BOOKING_BODY, SERVER));

        assertDoesNotThrow(() -> {
            steps.verifyResponseTimeWithinAgreedMaximum();
            steps.verifyAgreedFormatAndEncoding();
            steps.verifyServerIdentified();
        });
    }

    @Test
    @DisplayName("H-02: the selection does not pass when the booking at the reference value was never evaluated")
    void selectionRequiresDetailsOfEveryBooking() {
        ScenarioContext context = searchContext();
        int referenceValue = context.config().queryPriceThreshold();
        context.miguel().remember(FOUND_BOOKING_DETAILS, List.of(
                detail(1, referenceValue - 1), detail(3, referenceValue + 1), detail(4, referenceValue + 500)));
        context.miguel().remember(SELECTED_BOOKING_IDS, List.of(3, 4));

        assertThrows(AssertionError.class, new BookingSearchSteps(context)::verifyOnlyBookingsAboveReferenceValueIdentified);
    }

    @Test
    @DisplayName("the selection passes when every booking was evaluated and only those above the value were selected")
    void selectionPassesOverCompleteDetails() {
        ScenarioContext context = searchContext();
        int referenceValue = context.config().queryPriceThreshold();
        context.miguel().remember(FOUND_BOOKING_DETAILS, List.of(detail(1, referenceValue - 1),
                detail(2, referenceValue), detail(3, referenceValue + 1), detail(4, referenceValue + 500)));
        context.miguel().remember(SELECTED_BOOKING_IDS, List.of(3, 4));

        assertDoesNotThrow(new BookingSearchSteps(context)::verifyOnlyBookingsAboveReferenceValueIdentified);
    }

    private static BookingQualitySteps qualityStepsWith(Response bookingDetailsResponse) {
        ScenarioContext context = new ScenarioContext();
        context.miguel().remember(BOOKING_DETAILS_RESPONSE, bookingDetailsResponse);
        return new BookingQualitySteps(context);
    }

    private static ScenarioContext searchContext() {
        ScenarioContext context = new ScenarioContext();
        int referenceValue = context.config().queryPriceThreshold();
        Map<Integer, Booking> created = new LinkedHashMap<>();
        created.put(1, booking(referenceValue - 1));
        created.put(2, booking(referenceValue));
        created.put(3, booking(referenceValue + 1));
        created.put(4, booking(referenceValue + 500));
        context.miguel().remember(CREATED_BOOKINGS_BY_ID, created);
        return context;
    }

    private static BookingResponse detail(int bookingId, int price) {
        return BookingResponse.builder().bookingId(bookingId).booking(booking(price)).build();
    }

    private static Booking booking(int price) {
        return Booking.builder().firstName("Lamar-abc").totalPrice(price).build();
    }
}
