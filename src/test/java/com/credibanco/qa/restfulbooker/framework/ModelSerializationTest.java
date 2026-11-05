package com.credibanco.qa.restfulbooker.framework;

import com.credibanco.qa.restfulbooker.http.JsonMapper;
import com.credibanco.qa.restfulbooker.model.AuthRequest;
import com.credibanco.qa.restfulbooker.model.AuthResponse;
import com.credibanco.qa.restfulbooker.model.Booking;
import com.credibanco.qa.restfulbooker.model.BookingDates;
import com.credibanco.qa.restfulbooker.model.BookingResponse;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheBooking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DTO construction and (de)serialization against the API contract (RC-01, RC-02). No network.
 */
@DisplayName("Model serialization")
class ModelSerializationTest {

    private static final ObjectMapper MAPPER = JsonMapper.instance();

    private static Booking sampleBooking() {
        return Booking.builder()
                .firstName("Lamar-3f9a1c2e")
                .lastName("Smith")
                .totalPrice(321)
                .depositPaid(true)
                .bookingDates(BookingDates.builder()
                        .checkIn(LocalDate.of(2027, 1, 2))
                        .checkOut(LocalDate.of(2027, 1, 5))
                        .build())
                .additionalNeeds("Breakfast")
                .build();
    }

    @Test
    @DisplayName("serializes a booking with the exact contract field names, types and ISO dates")
    void serializesBookingWithContractFieldNames() {
        JsonNode json = MAPPER.valueToTree(sampleBooking());

        assertAll(
                () -> assertEquals(Set.of("firstname", "lastname", "totalprice", "depositpaid", "bookingdates",
                        "additionalneeds"), fieldNames(json)),
                () -> assertEquals(Set.of("checkin", "checkout"), fieldNames(json.get("bookingdates"))),
                () -> assertTrue(json.get("totalprice").isInt()),
                () -> assertTrue(json.get("depositpaid").isBoolean()),
                () -> assertEquals("2027-01-02", json.get("bookingdates").get("checkin").asText()),
                () -> assertEquals("2027-01-05", json.get("bookingdates").get("checkout").asText()));
    }

    @Test
    @DisplayName("omits absent fields instead of sending null")
    void omitsAbsentFields() {
        Booking withoutNeeds = sampleBooking().toBuilder().additionalNeeds(null).build();

        JsonNode json = MAPPER.valueToTree(withoutNeeds);

        assertFalse(json.has("additionalneeds"), json.toString());
    }

    @Test
    @DisplayName("round-trips a booking without losing data")
    void roundTripsBooking() throws IOException {
        Booking original = sampleBooking();

        Booking copy = MAPPER.readValue(MAPPER.writeValueAsString(original), Booking.class);

        assertEquals(original, copy);
    }

    @Test
    @DisplayName("deserializes the observed POST /booking response")
    void deserializesBookingResponseSample() throws IOException {
        BookingResponse response = read("booking-response.json", BookingResponse.class);

        Booking booking = response.getBooking();
        assertAll(
                () -> assertEquals(4206, response.getBookingId()),
                () -> assertEquals("QaProbe", booking.getFirstName()),
                () -> assertEquals("Analysis", booking.getLastName()),
                () -> assertEquals(321, booking.getTotalPrice()),
                () -> assertEquals(Boolean.TRUE, booking.getDepositPaid()),
                () -> assertEquals(LocalDate.of(2026, 12, 1), booking.getBookingDates().getCheckIn()),
                () -> assertEquals(LocalDate.of(2026, 12, 5), booking.getBookingDates().getCheckOut()),
                () -> assertEquals("Breakfast", booking.getAdditionalNeeds()));
    }

    @Test
    @DisplayName("tolerates unknown fields (the contract is checked by JSON Schema, not by the mapper)")
    void toleratesUnknownFields() throws IOException {
        ObjectNode withExtraField = MAPPER.valueToTree(sampleBooking());
        withExtraField.put("unexpected", "value");

        Booking booking = MAPPER.treeToValue(withExtraField, Booking.class);

        assertEquals(sampleBooking(), booking);
    }

    @Test
    @DisplayName("builders produce independent immutable copies")
    void toBuilderCreatesIndependentCopy() {
        Booking original = sampleBooking();

        Booking changed = original.toBuilder().totalPrice(999).build();

        assertAll(
                () -> assertEquals(321, original.getTotalPrice()),
                () -> assertEquals(999, changed.getTotalPrice()),
                () -> assertNotEquals(original, changed));
    }

    @Test
    @DisplayName("serializes the auth request and never exposes the password in toString")
    void serializesAuthRequestWithoutLeakingPassword() {
        AuthRequest request = AuthRequest.builder().username("user-x").password("secret-x").build();

        JsonNode json = MAPPER.valueToTree(request);

        assertAll(
                () -> assertEquals(Set.of("username", "password"), fieldNames(json)),
                () -> assertEquals("secret-x", json.get("password").asText()),
                () -> assertFalse(request.toString().contains("secret-x"), request.toString()));
    }

    @Test
    @DisplayName("deserializes a successful auth response and never exposes the token in toString")
    void deserializesAuthToken() throws IOException {
        AuthResponse response = read("auth-token.json", AuthResponse.class);

        assertAll(
                () -> assertEquals("abc123def456789", response.getToken()),
                () -> assertNull(response.getReason()),
                () -> assertFalse(response.toString().contains("abc123def456789"), response.toString()));
    }

    @Test
    @DisplayName("deserializes the bad-credentials response (KD-01) with no token")
    void deserializesBadCredentials() throws IOException {
        AuthResponse response = read("auth-bad-credentials.json", AuthResponse.class);

        assertAll(
                () -> assertNull(response.getToken()),
                () -> assertEquals("Bad credentials", response.getReason()));
    }

    @Test
    @DisplayName("H-04: rejects a price sent as text instead of converting it to a number")
    void rejectsPriceAsString() {
        String json = MAPPER.valueToTree(sampleBooking()).toString().replace("\"totalprice\":321", "\"totalprice\":\"321\"");

        assertThrows(JsonProcessingException.class, () -> MAPPER.readValue(json, Booking.class));
    }

    @Test
    @DisplayName("H-04: rejects a decimal price instead of truncating it to an integer")
    void rejectsDecimalPrice() {
        String json = MAPPER.valueToTree(sampleBooking()).toString().replace("\"totalprice\":321", "\"totalprice\":321.7");

        assertThrows(JsonProcessingException.class, () -> MAPPER.readValue(json, Booking.class));
    }

    @Test
    @DisplayName("H-04: rejects a deposit flag sent as text instead of converting it to a boolean")
    void rejectsDepositAsString() {
        String json = MAPPER.valueToTree(sampleBooking()).toString().replace("\"depositpaid\":true", "\"depositpaid\":\"true\"");

        assertThrows(JsonProcessingException.class, () -> MAPPER.readValue(json, Booking.class));
    }

    @Test
    @DisplayName("H-04: responses read through Rest Assured use the same strict mapping")
    void restAssuredUsesStrictMapping() {
        String json = MAPPER.valueToTree(sampleBooking()).toString().replace("\"totalprice\":321", "\"totalprice\":\"321\"");
        Actor actor = new StubbedApi().on("GET", "/booking/7", 200, json).actor("Guest");

        assertThrows(RuntimeException.class, () -> actor.asksFor(TheBooking.withId(7)));
    }

    private static Set<String> fieldNames(JsonNode node) {
        return StreamSupport.stream(((Iterable<String>) node::fieldNames).spliterator(), false)
                .collect(Collectors.toSet());
    }

    private static <T> T read(String fixture, Class<T> type) throws IOException {
        try (InputStream stream = ModelSerializationTest.class.getResourceAsStream("/fixtures/" + fixture)) {
            return MAPPER.readValue(stream, type);
        }
    }
}
