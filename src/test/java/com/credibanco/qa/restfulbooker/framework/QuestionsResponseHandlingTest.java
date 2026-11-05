package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.UnexpectedResponseException;
import com.credibanco.qa.restfulbooker.screenplay.questions.BookingDetails;
import com.credibanco.qa.restfulbooker.screenplay.questions.BookingIds;
import com.credibanco.qa.restfulbooker.screenplay.questions.HighestBookingId;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheBooking;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HTTP Questions: not found vs. unexpected responses")
class QuestionsResponseHandlingTest {

    private static final String BOOKING = """
            {"firstname":"Lamar-abc","lastname":"Smith","totalprice":321,"depositpaid":true,
            "bookingdates":{"checkin":"2027-01-02","checkout":"2027-01-05"},"additionalneeds":"Breakfast"}""";
    private static final String SERVER_ERROR = "{\"error\":\"Internal Server Error\"}";

    @Test
    @DisplayName("a booking that answers 404 does not exist")
    void notFoundIsEmpty() {
        Actor actor = new StubbedApi().on("GET", "/booking/7", 404, "text/plain", "Not Found").actor("Guest");

        assertEquals(Optional.empty(), actor.asksFor(TheBooking.withId(7)));
    }

    @Test
    @DisplayName("a booking that answers 200 is returned")
    void foundIsReturned() {
        Actor actor = new StubbedApi().on("GET", "/booking/7", 200, BOOKING).actor("Guest");

        assertEquals("Lamar-abc", actor.asksFor(TheBooking.withId(7)).orElseThrow().getFirstName());
    }

    @Test
    @DisplayName("a booking read that answers 500 is an unexpected response, not a missing booking")
    void serverErrorOnBookingIsUnexpected() {
        Actor actor = new StubbedApi().on("GET", "/booking/7", 500, SERVER_ERROR).actor("Guest");

        UnexpectedResponseException error = assertThrows(UnexpectedResponseException.class,
                () -> actor.asksFor(TheBooking.withId(7)));

        assertAll(
                () -> assertEquals(500, error.statusCode()),
                () -> assertTrue(error.getMessage().contains("HTTP 500"), error.getMessage()));
    }

    @Test
    @DisplayName("H-02: the details of several bookings never silently drop one that answered an error")
    void detailsDoNotDropFailedReads() {
        Actor actor = new StubbedApi()
                .on("GET", "/booking/1", 200, BOOKING)
                .on("GET", "/booking/2", 503, "text/html", "<html>Service Unavailable</html>")
                .actor("Guest");

        assertThrows(UnexpectedResponseException.class, () -> actor.asksFor(BookingDetails.of(List.of(1, 2))));
    }

    @Test
    @DisplayName("the details of several bookings leave out only those that no longer exist")
    void detailsLeaveOutOnlyNotFound() {
        Actor actor = new StubbedApi()
                .on("GET", "/booking/1", 200, BOOKING)
                .on("GET", "/booking/2", 404, "text/plain", "Not Found")
                .actor("Guest");

        List<BookingResponse> details = actor.asksFor(BookingDetails.of(List.of(1, 2)));

        assertEquals(List.of(1), details.stream().map(BookingResponse::getBookingId).toList());
    }

    @Test
    @DisplayName("M-03: a search that answers 500 is an unexpected response, not an empty search result")
    void searchErrorIsUnexpected() {
        Actor actor = new StubbedApi().on("GET", "/booking", 500, SERVER_ERROR).actor("Guest");

        assertThrows(UnexpectedResponseException.class, () -> actor.asksFor(BookingIds.withFirstName("Lamar-abc")));
    }

    @Test
    @DisplayName("a search that answers 200 returns the booking ids")
    void searchReturnsIds() {
        Actor actor = new StubbedApi().on("GET", "/booking", 200, "[{\"bookingid\":3},{\"bookingid\":9}]").actor("Guest");

        assertEquals(List.of(3, 9), actor.asksFor(BookingIds.withFirstName("Lamar-abc")));
    }

    @Test
    @DisplayName("M-03: the highest booking id is not answered when the list cannot be read")
    void highestIdErrorIsUnexpected() {
        Actor actor = new StubbedApi().on("GET", "/booking", 503, "text/html", "<html>Service Unavailable</html>")
                .actor("Guest");

        assertThrows(UnexpectedResponseException.class, () -> actor.asksFor(HighestBookingId.current()));
    }

    @Test
    @DisplayName("the highest booking id is the maximum of the list, or 0 when there are none")
    void highestIdFromList() {
        Actor withBookings = new StubbedApi().on("GET", "/booking", 200, "[{\"bookingid\":3},{\"bookingid\":9}]")
                .actor("Guest");
        Actor withoutBookings = new StubbedApi().on("GET", "/booking", 200, "[]").actor("Guest");

        assertAll(
                () -> assertEquals(9, (Object) withBookings.asksFor(HighestBookingId.current())),
                () -> assertEquals(0, (Object) withoutBookings.asksFor(HighestBookingId.current())));
    }
}
