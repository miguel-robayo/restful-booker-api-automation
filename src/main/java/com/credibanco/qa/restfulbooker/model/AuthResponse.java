package com.credibanco.qa.restfulbooker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Response of {@code POST /auth}: {@code token} on success, or {@code reason} with HTTP 200 on bad credentials
 * (KD-01). Both fields are optional so either shape deserializes. The token never appears in {@code toString}.
 */
@Value
@Builder
@Jacksonized
public class AuthResponse {

    @ToString.Exclude
    @JsonProperty("token")
    String token;

    @JsonProperty("reason")
    String reason;
}
