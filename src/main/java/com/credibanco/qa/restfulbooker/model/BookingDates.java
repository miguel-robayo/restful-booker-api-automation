package com.credibanco.qa.restfulbooker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

/**
 * {@code bookingdates} of RC-01. Dates travel as {@code CCYY-MM-DD}.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class BookingDates {

    @JsonProperty("checkin")
    LocalDate checkIn;

    @JsonProperty("checkout")
    LocalDate checkOut;
}
