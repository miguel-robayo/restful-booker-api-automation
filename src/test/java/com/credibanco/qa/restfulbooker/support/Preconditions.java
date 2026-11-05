package com.credibanco.qa.restfulbooker.support;

import com.credibanco.qa.restfulbooker.screenplay.core.PreconditionFailedException;
import com.credibanco.qa.restfulbooker.screenplay.core.UnexpectedResponseException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Checks used by "Dado" steps: when the scenario cannot be prepared, the result is a precondition failure (D-09),
 * not a functional failure.
 */
public final class Preconditions {

    private Preconditions() {
    }

    public static void require(boolean condition, String explanation) {
        if (!condition) {
            throw new PreconditionFailedException(explanation);
        }
    }

    public static <T> T present(Optional<T> value, String explanation) {
        return value.orElseThrow(() -> new PreconditionFailedException(explanation));
    }

    public static <T> T obtained(Supplier<T> read, String explanation) {
        try {
            return read.get();
        } catch (UnexpectedResponseException unexpected) {
            throw new PreconditionFailedException(explanation + ": " + unexpected.getMessage(), unexpected);
        }
    }
}
