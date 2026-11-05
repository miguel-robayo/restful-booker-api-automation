package com.credibanco.qa.restfulbooker.steps;

import com.credibanco.qa.restfulbooker.context.ScenarioContext;
import com.credibanco.qa.restfulbooker.http.SentRequest;
import com.credibanco.qa.restfulbooker.model.AuthResponse;
import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import com.credibanco.qa.restfulbooker.screenplay.abilities.UseAnAuthToken;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheLastResponse;
import com.credibanco.qa.restfulbooker.screenplay.questions.TheSentRequests;
import com.credibanco.qa.restfulbooker.screenplay.tasks.Authenticate;
import com.credibanco.qa.restfulbooker.support.Expect;
import com.credibanco.qa.restfulbooker.support.HttpStatus;
import com.credibanco.qa.restfulbooker.support.Preconditions;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.AUTHENTICATION_RESPONSE;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.AUTHENTICATION_TASK;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.OWN_BOOKING_ID;
import static com.credibanco.qa.restfulbooker.context.ScenarioMemory.SESSION_TOKEN_FINGERPRINT;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sign-in and session steps. Business vocabulary: "iniciar sesión" = obtain an auth token; "sesión" = that token.
 */
public class AuthenticationSteps {

    private static final String AUTH_PATH = "/auth";
    private static final String BOOKING_PATH = "/booking/";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";

    private final Actor miguel;

    public AuthenticationSteps(ScenarioContext context) {
        this.miguel = context.miguel();
    }

    // ---------- Dado ----------

    /** Validates that credentials are configured (fails fast otherwise); no request is sent yet. */
    @Dado("que Miguel tiene credenciales de acceso válidas")
    public void prepareValidCredentials() {
        miguel.remember(AUTHENTICATION_TASK, Authenticate.withConfiguredCredentials());
    }

    @Dado("que Miguel inició sesión")
    public void startSession() {
        miguel.wasAbleTo(Authenticate.withConfiguredCredentials());
        miguel.remember(SESSION_TOKEN_FINGERPRINT, UseAnAuthToken.as(miguel).fingerprint());
    }

    @Dado("no ha iniciado sesión")
    public void requireNoSession() {
        Preconditions.require(!miguel.hasAbilityTo(UseAnAuthToken.class), "Miguel must not hold any session token");
    }

    @Dado("utiliza una sesión que el sistema no emitió")
    public void useSessionNotIssuedBySystem() {
        miguel.can(UseAnAuthToken.invalid());
        Preconditions.require(miguel.hasAbilityTo(UseAnAuthToken.class), "Miguel must hold the session not issued by the system");
    }

    // ---------- Cuando ----------

    @Cuando("inicia sesión")
    public void signIn() {
        miguel.attemptsTo(miguel.<Authenticate>recall(AUTHENTICATION_TASK));
        miguel.remember(AUTHENTICATION_RESPONSE, miguel.asksFor(TheLastResponse.raw()));
    }

    // ---------- Entonces (no HTTP: stored evidence only) ----------

    @Entonces("el sistema reconoce su identidad")
    public void verifyIdentityRecognized() {
        Response authenticationResponse = miguel.recall(AUTHENTICATION_RESPONSE);
        Expect.status(authenticationResponse, HttpStatus.OK, "POST /auth");
        String rejectionReason = authenticationResponse.as(AuthResponse.class).getReason();
        assertNull(rejectionReason, "no rejection reason expected, got: " + rejectionReason);
    }

    @Entonces("le concede una sesión para gestionar sus reservas")
    public void verifySessionGranted() {
        String authenticationToken = miguel.<Response>recall(AUTHENTICATION_RESPONSE).as(AuthResponse.class).getToken();
        assertAll(
                () -> assertNotNull(authenticationToken, "a session token must be issued"),
                () -> assertFalse(authenticationToken.isBlank(), "the session token must not be blank"));
    }

    @Entonces("la modificación y la cancelación se realizaron con la misma sesión")
    public void verifyModificationAndCancellationUsedSameSession() {
        List<SentRequest> sentRequests = miguel.asksFor(TheSentRequests.all());
        String bookingPath = BOOKING_PATH + miguel.<Integer>recall(OWN_BOOKING_ID);
        Optional<String> modificationToken = tokenSentBy(sentRequests, PUT, bookingPath);
        Optional<String> cancellationToken = tokenSentBy(sentRequests, DELETE, bookingPath);
        String sessionToken = miguel.recall(SESSION_TOKEN_FINGERPRINT);
        AllureReporter.attach("Token identity per request (fingerprints, never the token)",
                "Obtained at sign-in: " + sessionToken + "\n"
                        + sentRequests.stream().map(SentRequest::toString).collect(Collectors.joining("\n")));
        assertAll(
                () -> assertTrue(modificationToken.isPresent(), "the modification must carry a session token"),
                () -> assertTrue(cancellationToken.isPresent(), "the cancellation must carry a session token"),
                () -> assertEquals(modificationToken, cancellationToken, "modification and cancellation must use the same token"),
                () -> assertEquals(Optional.of(sessionToken), modificationToken, "the token must be the one obtained at sign-in"));
    }

    @Entonces("no fue necesario iniciar sesión nuevamente")
    public void verifySingleSignIn() {
        long signIns = miguel.asksFor(TheSentRequests.all()).stream()
                .filter(request -> request.method().equals(POST) && request.path().equals(AUTH_PATH))
                .count();
        assertEquals(1, signIns, "POST /auth must be sent exactly once");
    }

    private static Optional<String> tokenSentBy(List<SentRequest> sentRequests, String method, String path) {
        return sentRequests.stream()
                .filter(request -> request.method().equals(method) && request.path().equals(path))
                .findFirst()
                .flatMap(SentRequest::tokenFingerprint);
    }
}
