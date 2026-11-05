package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.data.BookingDataFactory;
import com.credibanco.qa.restfulbooker.data.UniqueMarker;
import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingDates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data rules of the test strategy (§4): valid, unique and reproducible. No network.
 */
@DisplayName("Booking data factory")
class BookingDataFactoryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);
    private static final int SAMPLES = 300;
    private static final List<String> ADDITIONAL_NEEDS = List.of("Breakfast", "Late checkout", "Airport transfer", "Extra bed");

    @Test
    @DisplayName("generates bookings that respect every data rule")
    void generatesValidBookings() {
        BookingDataFactory factory = BookingDataFactory.withSeed(7, FIXED_CLOCK);

        for (int i = 0; i < SAMPLES; i++) {
            Booking booking = factory.aValidBooking();
            BookingDates dates = booking.getBookingDates();
            long daysAhead = ChronoUnit.DAYS.between(TODAY, dates.getCheckIn());
            long nights = ChronoUnit.DAYS.between(dates.getCheckIn(), dates.getCheckOut());
            assertAll(booking.toString(),
                    () -> assertTrue(booking.getFirstName().matches("[A-Za-z]+-[0-9a-f]{12}")),
                    () -> assertTrue(booking.getLastName().matches("[A-Za-z]+")),
                    () -> assertTrue(booking.getTotalPrice() >= 1 && booking.getTotalPrice() <= 10_000),
                    () -> assertNotNull(booking.getDepositPaid()),
                    () -> assertTrue(daysAhead >= 1 && daysAhead <= 365),
                    () -> assertTrue(nights >= 1 && nights <= 14),
                    () -> assertTrue(ADDITIONAL_NEEDS.contains(booking.getAdditionalNeeds())));
        }
    }

    @Test
    @DisplayName("gives every booking a unique first name")
    void generatesUniqueFirstNames() {
        BookingDataFactory factory = BookingDataFactory.withSeed(7, FIXED_CLOCK);

        Set<String> names = new HashSet<>();
        IntStream.range(0, SAMPLES).forEach(i -> names.add(factory.aValidBooking().getFirstName()));

        assertEquals(SAMPLES, names.size());
    }

    @Test
    @DisplayName("reproduces the same data for the same seed, except the unique marker")
    void reproducesDataForSameSeed() {
        Booking first = BookingDataFactory.withSeed(42, FIXED_CLOCK).aValidBooking();
        Booking second = BookingDataFactory.withSeed(42, FIXED_CLOCK).aValidBooking();

        assertAll(
                () -> assertEquals(baseName(first), baseName(second)),
                () -> assertNotEquals(first.getFirstName(), second.getFirstName(), "marker must stay unique"),
                () -> assertEquals(withoutFirstName(first), withoutFirstName(second)));
    }

    @Test
    @DisplayName("produces different data for different seeds")
    void differsForDifferentSeeds() {
        Booking first = BookingDataFactory.withSeed(1, FIXED_CLOCK).aValidBooking();
        Booking second = BookingDataFactory.withSeed(2, FIXED_CLOCK).aValidBooking();

        assertNotEquals(withoutFirstName(first), withoutFirstName(second));
    }

    @Test
    @DisplayName("exposes its seed so a run can be replayed")
    void exposesSeed() {
        assertEquals(42L, BookingDataFactory.withSeed(42).seed());
    }

    @Test
    @DisplayName("builds a valid booking with a requested price")
    void buildsBookingWithRequestedPrice() {
        Booking booking = BookingDataFactory.withSeed(7, FIXED_CLOCK).aBookingPricedAt(500);

        assertAll(
                () -> assertEquals(500, booking.getTotalPrice()),
                () -> assertNotNull(booking.getBookingDates()),
                () -> assertNotNull(booking.getAdditionalNeeds()));
    }

    @Test
    @DisplayName("builds valid bookings that share a given first name")
    void buildsBookingsSharingFirstName() {
        BookingDataFactory factory = BookingDataFactory.withSeed(7, FIXED_CLOCK);
        String marker = factory.uniqueFirstName();

        Booking first = factory.aBookingWithFirstName(marker);
        Booking second = factory.aBookingWithFirstName(marker);

        assertAll(
                () -> assertEquals(marker, first.getFirstName()),
                () -> assertEquals(marker, second.getFirstName()),
                () -> assertNotEquals(first, second));
    }

    @Test
    @DisplayName("generates unique markers even across threads")
    void generatesUniqueMarkersAcrossThreads() {
        int count = 10_000;

        long distinct = IntStream.range(0, count).parallel().mapToObj(i -> UniqueMarker.next()).distinct().count();

        assertEquals(count, distinct);
    }

    private static String baseName(Booking booking) {
        return booking.getFirstName().substring(0, booking.getFirstName().indexOf('-'));
    }

    private static Booking withoutFirstName(Booking booking) {
        return booking.toBuilder().firstName(null).build();
    }
}
