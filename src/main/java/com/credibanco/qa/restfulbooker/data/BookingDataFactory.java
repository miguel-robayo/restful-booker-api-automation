package com.credibanco.qa.restfulbooker.data;

import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingDates;
import net.datafaker.Faker;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Single source of valid, unique and reproducible booking data (test strategy §4, D-04).
 *
 * <p>Rules: price 1..10 000; check-in 1..365 days ahead; check-out 1..14 nights after check-in; every RC-01 field
 * present. The same seed and clock reproduce the same data; only the {@link UniqueMarker} suffix of the first name
 * changes, so replayed data never collides on the shared API.
 *
 * <p>Not thread-safe: create one instance per scenario.
 */
public final class BookingDataFactory {

    static final int MIN_PRICE = 1;
    static final int MAX_PRICE = 10_000;
    static final int MAX_DAYS_AHEAD = 365;
    static final int MAX_NIGHTS = 14;
    static final List<String> ADDITIONAL_NEEDS = List.of("Breakfast", "Late checkout", "Airport transfer", "Extra bed");

    private static final String FALLBACK_NAME = "Guest";

    private final long seed;
    private final Random random;
    private final Faker faker;
    private final Clock clock;

    private BookingDataFactory(long seed, Clock clock) {
        this.seed = seed;
        this.random = new Random(seed);
        this.faker = new Faker(Locale.ENGLISH, random);
        this.clock = clock;
    }

    public static BookingDataFactory withSeed(long seed) {
        return withSeed(seed, Clock.systemDefaultZone());
    }

    public static BookingDataFactory withSeed(long seed, Clock clock) {
        return new BookingDataFactory(seed, clock);
    }

    /** Seed to report as evidence and to replay with {@code -Ddata.seed}. */
    public long seed() {
        return seed;
    }

    public Booking aValidBooking() {
        return aBookingWithFirstName(uniqueFirstName());
    }

    /** Valid booking with a given first name, e.g. several bookings sharing one marker for a filter scenario. */
    public Booking aBookingWithFirstName(String firstName) {
        LocalDate checkIn = LocalDate.now(clock).plusDays(random.nextInt(1, MAX_DAYS_AHEAD + 1));
        return Booking.builder()
                .firstName(firstName)
                .lastName(lettersOnly(faker.name().lastName()))
                .totalPrice(random.nextInt(MIN_PRICE, MAX_PRICE + 1))
                .depositPaid(random.nextBoolean())
                .bookingDates(BookingDates.builder()
                        .checkIn(checkIn)
                        .checkOut(checkIn.plusDays(random.nextInt(1, MAX_NIGHTS + 1)))
                        .build())
                .additionalNeeds(ADDITIONAL_NEEDS.get(random.nextInt(ADDITIONAL_NEEDS.size())))
                .build();
    }

    public Booking aBookingPricedAt(int totalPrice) {
        return aValidBooking().toBuilder().totalPrice(totalPrice).build();
    }

    /** Valid booking with a given first name and price, e.g. bookings around a filter threshold. */
    public Booking aBookingWithFirstNameAndPrice(String firstName, int totalPrice) {
        return aBookingWithFirstName(firstName).toBuilder().totalPrice(totalPrice).build();
    }

    /** Generated first name plus a unique marker, e.g. {@code Lamar-3f9a1c2e7b04}. */
    public String uniqueFirstName() {
        return lettersOnly(faker.name().firstName()) + "-" + UniqueMarker.next();
    }

    private static String lettersOnly(String name) {
        String letters = name.replaceAll("[^A-Za-z]", "");
        return letters.isEmpty() ? FALLBACK_NAME : letters;
    }
}
