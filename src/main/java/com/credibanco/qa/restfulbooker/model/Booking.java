package com.credibanco.qa.restfulbooker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A booking as defined by RC-01. Used both as request body ({@code POST}/{@code PUT}) and as response of
 * {@code GET /booking/{id}}, because the contract has the same shape in both directions.
 *
 * <p>{@code totalprice} is an {@link Integer}: the API truncates decimals ({@code 123.45} is stored as {@code 123}).
 * Wrapper types allow a field to be absent, which some negative scenarios need.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class Booking {

    @JsonProperty("firstname")
    String firstName;

    @JsonProperty("lastname")
    String lastName;

    @JsonProperty("totalprice")
    Integer totalPrice;

    @JsonProperty("depositpaid")
    Boolean depositPaid;

    @JsonProperty("bookingdates")
    BookingDates bookingDates;

    @JsonProperty("additionalneeds")
    String additionalNeeds;
}
