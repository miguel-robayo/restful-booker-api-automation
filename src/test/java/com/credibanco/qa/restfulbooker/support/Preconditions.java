package com.credibanco.qa.restfulbooker.support;

import com.credibanco.qa.restfulbooker.screenplay.core.PreconditionFailedException;

import java.util.Optional;

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
}
