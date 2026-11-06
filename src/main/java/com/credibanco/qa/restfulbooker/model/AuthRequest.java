package com.credibanco.qa.restfulbooker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Body of {@code POST /auth}. The password never appears in {@code toString}.
 */
@Value
@Builder
@Jacksonized
public class AuthRequest {

    @JsonProperty("username")
    String username;

    @ToString.Exclude
    @JsonProperty("password")
    String password;
}
