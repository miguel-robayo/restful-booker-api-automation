package com.credibanco.qa.restfulbooker.steps;

import com.credibanco.qa.restfulbooker.context.ScenarioContext;
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

    @Dado("que no ha iniciado sesión")
    public void requireNoSession() {
        Preconditions.require(!miguel.hasAbilityTo(UseAnAuthToken.class), "Miguel must not hold any session token");
    }

    @Dado("que utiliza una sesión que el sistema no emitió")
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
        int ownBookingId = miguel.recall(OWN_BOOKING_ID);
        Optional<String> modificationToken = miguel.asksFor(TheSentRequests.sessionUsedToModify(ownBookingId));
        Optional<String> cancellationToken = miguel.asksFor(TheSentRequests.sessionUsedToCancel(ownBookingId));
        String sessionToken = miguel.recall(SESSION_TOKEN_FINGERPRINT);
        AllureReporter.attach("Token identity per request (fingerprints, never the token)",
                "Obtained at sign-in: " + sessionToken + "\n"
                        + miguel.asksFor(TheSentRequests.all()).stream().map(Object::toString).collect(Collectors.joining("\n")));
        assertAll(
                () -> assertTrue(modificationToken.isPresent(), "the modification must carry a session token"),
                () -> assertTrue(cancellationToken.isPresent(), "the cancellation must carry a session token"),
                () -> assertEquals(modificationToken, cancellationToken, "modification and cancellation must use the same token"),
                () -> assertEquals(Optional.of(sessionToken), modificationToken, "the token must be the one obtained at sign-in"));
    }

    @Entonces("no fue necesario iniciar sesión nuevamente")
    public void verifySingleSignIn() {
        assertEquals(1L, miguel.asksFor(TheSentRequests.signIns()), "POST /auth must be sent exactly once");
    }
}
