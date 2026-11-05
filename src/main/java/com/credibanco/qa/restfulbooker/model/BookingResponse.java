package com.credibanco.qa.restfulbooker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Response of {@code POST /booking} (RC-02).
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class BookingResponse {

    @JsonProperty("bookingid")
    Integer bookingId;

    @JsonProperty("booking")
    Booking booking;
}
