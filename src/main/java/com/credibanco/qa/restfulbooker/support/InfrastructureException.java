package com.credibanco.qa.restfulbooker.support;

/**
 * The environment is not available. The {@code INFRA:} prefix routes it to the Infrastructure category.
 */
public class InfrastructureException extends RuntimeException {

    private static final String PREFIX = "INFRA: ";

    public InfrastructureException(String message) {
        super(PREFIX + message);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(PREFIX + message, cause);
    }
}
