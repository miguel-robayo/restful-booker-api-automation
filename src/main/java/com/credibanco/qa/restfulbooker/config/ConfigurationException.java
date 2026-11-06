package com.credibanco.qa.restfulbooker.config;

/**
 * Raised when the test configuration is missing or invalid. The message always says how to fix it.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
