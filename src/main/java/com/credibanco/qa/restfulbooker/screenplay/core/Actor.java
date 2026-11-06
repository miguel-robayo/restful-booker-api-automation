package com.credibanco.qa.restfulbooker.screenplay.core;

import com.credibanco.qa.restfulbooker.http.ResponseSummary;
import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import io.restassured.response.Response;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The scenario's protagonist (ADR-13): {@link #wasAbleTo} prepares preconditions, {@link #attemptsTo} performs the
 * action under test, {@link #asksFor} reads. It remembers the last HTTP response and any value a step stores.
 *
 * <p>Not thread-safe by design: one actor per scenario, never shared (ADR-15).
 */
public final class Actor {

    private final String name;
    private final Map<Class<? extends Ability>, Ability> abilities = new HashMap<>();
    private final Map<String, Object> memory = new HashMap<>();
    private final Deque<List<Response>> openPreconditions = new ArrayDeque<>();
    private Response lastResponse;

    private Actor(String name) {
        this.name = name;
    }

    public static Actor named(String name) {
        return new Actor(name);
    }

    /** Grants an ability; a new ability of the same type replaces the previous one (e.g. a fresh token). */
    public Actor can(Ability ability) {
        abilities.put(ability.getClass(), ability);
        return this;
    }

    public boolean hasAbilityTo(Class<? extends Ability> type) {
        return abilities.containsKey(type);
    }

    public <T extends Ability> T abilityTo(Class<T> type) {
        Ability ability = abilities.get(type);
        if (ability == null) {
            throw new IllegalStateException(name + " does not have the ability " + type.getSimpleName());
        }
        return type.cast(ability);
    }

    /** Action under test: the outcome is recorded, never validated here. */
    public void attemptsTo(Performable... actions) {
        for (Performable action : actions) {
            AllureReporter.step(name + " attempts to " + action, () -> action.performAs(this));
        }
    }

    /**
     * Precondition: each action must succeed, otherwise {@link PreconditionFailedException} (D-09). Network failures
     * propagate unchanged so they are classified as Infrastructure.
     */
    public void wasAbleTo(Performable... actions) {
        for (Performable action : actions) {
            List<Response> ownResponses = perform(action);
            if (!succeeded(action, ownResponses)) {
                throw new PreconditionFailedException(action + " did not succeed: " + describe(ownResponses));
            }
        }
    }

    private List<Response> perform(Performable action) {
        List<Response> ownResponses = new ArrayList<>();
        openPreconditions.push(ownResponses);
        try {
            AllureReporter.step(name + " was able to " + action, () -> action.performAs(this));
        } catch (PreconditionFailedException failure) {
            throw failure;
        } catch (RuntimeException failure) {
            if (isNetworkFailure(failure)) {
                throw failure;
            }
            throw new PreconditionFailedException(action + " could not be completed: " + failure.getMessage(), failure);
        } finally {
            openPreconditions.pop();
        }
        return List.copyOf(ownResponses);
    }

    private static boolean succeeded(Performable action, List<Response> ownResponses) {
        try {
            return action.succeededWith(ownResponses);
        } catch (RuntimeException unreadable) {
            throw new PreconditionFailedException(action + " returned a response that could not be interpreted: "
                    + describe(ownResponses), unreadable);
        }
    }

    public <T> T asksFor(Question<T> question) {
        return AllureReporter.step(name + " asks for " + question, () -> question.answeredBy(this));
    }

    public void remember(String key, Object value) {
        memory.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T recall(String key) {
        if (!memory.containsKey(key)) {
            throw new NoSuchElementException(name + " does not remember '" + key + "'");
        }
        return (T) memory.get(key);
    }

    /** Called by Tasks and Questions after every HTTP call. */
    public void recordResponse(Response response) {
        this.lastResponse = response;
        openPreconditions.forEach(ownResponses -> ownResponses.add(response));
    }

    public Optional<Response> lastResponse() {
        return Optional.ofNullable(lastResponse);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    private static String describe(List<Response> ownResponses) {
        return ownResponses.isEmpty()
                ? "it received no response"
                : ownResponses.stream().map(ResponseSummary::of).collect(Collectors.joining(" | "));
    }

    private static boolean isNetworkFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof IOException) {
                return true;
            }
        }
        return false;
    }
}
