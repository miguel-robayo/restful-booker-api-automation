package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;

import java.util.List;
import java.util.Optional;

/**
 * Real details of a list of bookings, one {@code GET /booking/{id}} each (D-01: {@code GET /booking} only returns
 * ids). Built with a stream, without loops. Bookings that no longer exist are left out, never invented.
 */
public final class BookingDetails {

    private BookingDetails() {
    }

    public static Question<List<BookingResponse>> of(List<Integer> bookingIds) {
        return Question.about("the details of bookings " + bookingIds, actor -> bookingIds.stream()
                .map(id -> detail(id, TheBooking.withId(id).answeredBy(actor)))
                .flatMap(Optional::stream)
                .toList());
    }

    private static Optional<BookingResponse> detail(int bookingId, Optional<Booking> booking) {
        return booking.map(found -> BookingResponse.builder().bookingId(bookingId).booking(found).build());
    }
}
