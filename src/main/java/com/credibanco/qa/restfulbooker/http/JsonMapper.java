package com.credibanco.qa.restfulbooker.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * The single Jackson configuration of the framework (ADR-09, ADR-10).
 *
 * <ul>
 *   <li>Unknown properties are ignored: the contract is validated with JSON Schema, not by the mapper (D-15).</li>
 *   <li>{@code null} fields are not serialized, so an absent field is really absent from the request.</li>
 *   <li>{@link java.time.LocalDate} is written as {@code CCYY-MM-DD}.</li>
 * </ul>
 *
 * <p>The instance is thread-safe once configured; callers must not reconfigure it.
 */
public final class JsonMapper {

    private static final ObjectMapper INSTANCE = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    private JsonMapper() {
    }

    public static ObjectMapper instance() {
        return INSTANCE;
    }
}
