package com.credibanco.qa.restfulbooker.support;

import java.util.List;

/**
 * What happened to each booking a scenario created. Contains only ids, synthetic first names and reasons: never
 * tokens or credentials.
 */
public record CleanupResult(List<Outcome> outcomes) {

    public enum Status { DELETED, ALREADY_GONE, NOT_OURS, PENDING }

    public record Outcome(int bookingId, String firstName, Status status, String detail) {
    }

    public static CleanupResult nothingToClean() {
        return new CleanupResult(List.of());
    }

    public List<Outcome> pending() {
        return outcomes.stream().filter(outcome -> outcome.status() == Status.PENDING).toList();
    }
}
