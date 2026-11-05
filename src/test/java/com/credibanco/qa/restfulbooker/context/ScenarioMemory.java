package com.credibanco.qa.restfulbooker.context;

/**
 * Keys of what Miguel remembers during a scenario: a step stores a result or a piece of evidence under a key, a
 * later step reads it.
 */
public final class ScenarioMemory {

    // Authentication
    public static final String AUTHENTICATION_TASK = "authentication with the configured credentials";
    public static final String AUTHENTICATION_RESPONSE = "authentication response";
    public static final String SESSION_TOKEN_FINGERPRINT = "fingerprint of the session token";

    // Own booking
    public static final String BOOKING_REQUEST = "booking request";
    public static final String CREATION_RESPONSE = "creation response";
    public static final String OWN_BOOKING = "own booking";
    public static final String OWN_BOOKING_ID = "own booking id";
    public static final String INITIAL_BOOKING_STATE = "initial booking state";

    // Modification and cancellation
    public static final String UPDATED_BOOKING_DETAILS = "updated booking details";
    public static final String UPDATE_RESPONSE = "update response";
    public static final String CANCELLATION_RESPONSE = "cancellation response";
    public static final String BOOKING_STATE_AFTER_ACTION = "booking state after the action";
    public static final String BOOKING_READ_AFTER_ACTION = "response of reading the booking after the action";

    // Details of a booking
    public static final String BOOKING_DETAILS_RESPONSE = "booking details response";

    // Search by value
    public static final String GUEST_UNIQUE_NAME = "unique first name of Miguel's bookings";
    public static final String CREATED_BOOKINGS_BY_ID = "created bookings by id";
    public static final String FOUND_BOOKING_IDS = "booking ids found by name";
    public static final String FOUND_BOOKING_DETAILS = "details of the found bookings";
    public static final String SELECTED_BOOKING_IDS = "booking ids above the reference value";
    public static final String IDENTIFIED_BOOKING_ID = "identified booking id";
    public static final String IDENTIFIED_BOOKING = "identified booking";

    // Nonexistent booking
    public static final String NONEXISTENT_BOOKING_ID = "nonexistent booking id";
    public static final String NONEXISTENT_BOOKING_RESPONSE = "response of reading the nonexistent booking";

    private ScenarioMemory() {
    }
}
