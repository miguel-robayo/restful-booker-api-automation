package com.credibanco.qa.restfulbooker.api;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * {@code /booking} endpoints. Payloads are {@link Object} on purpose: valid bookings use the model, and negative
 * scenarios send test-only payloads that the model cannot represent (e.g. a non-numeric price).
 */
public final class BookingApi extends BaseApi {

    private static final String BOOKINGS = "/booking";
    private static final String BOOKING_BY_ID = "/booking/{id}";
    private static final String ID = "id";

    public BookingApi(RequestSpecification spec) {
        super(spec);
    }

    public Response create(Object payload) {
        return request().body(payload).post(BOOKINGS);
    }

    public Response getById(long bookingId) {
        return request().pathParam(ID, bookingId).get(BOOKING_BY_ID);
    }

    /** {@code GET /booking} with optional filters: firstname, lastname, checkin, checkout (RC-03). */
    public Response findIds(Map<String, ?> filters) {
        return request().queryParams(filters).get(BOOKINGS);
    }

    public Response update(long bookingId, Object payload) {
        return request().pathParam(ID, bookingId).body(payload).put(BOOKING_BY_ID);
    }

    public Response delete(long bookingId) {
        return request().pathParam(ID, bookingId).delete(BOOKING_BY_ID);
    }
}
