package com.credibanco.qa.restfulbooker.screenplay.core;

/**
 * A scenario could not be prepared (D-09). Not an {@link AssertionError}, so Allure reports it as broken, and the
 * {@code PRECONDITION:} prefix routes it to the Precondition category.
 */
public class PreconditionFailedException extends RuntimeException {

    static final String PREFIX = "PRECONDITION: ";

    public PreconditionFailedException(String message) {
        super(PREFIX + message);
    }

    public PreconditionFailedException(String message, Throwable cause) {
        super(PREFIX + message, cause);
    }
}
