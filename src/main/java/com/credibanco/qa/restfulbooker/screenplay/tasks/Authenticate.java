package com.credibanco.qa.restfulbooker.screenplay.tasks;

import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.model.AuthRequest;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.abilities.UseAnAuthToken;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Performable;
import io.restassured.response.Response;

/**
 * {@code POST /auth}. A returned token becomes the actor's {@link UseAnAuthToken}. The API answers 200 even for bad
 * credentials (KD-01), so success means "a token was issued", not just "2xx".
 */
public final class Authenticate implements Performable {

    private static final String TOKEN = "token";

    private final AuthRequest credentials;
    private boolean tokenIssued;

    private Authenticate(AuthRequest credentials) {
        this.credentials = credentials;
    }

    /** Credentials from RB_USERNAME / RB_PASSWORD; fails fast here if they are missing. */
    public static Authenticate withConfiguredCredentials() {
        EnvironmentConfig config = EnvironmentConfig.current();
        return with(AuthRequest.builder().username(config.username()).password(config.password()).build());
    }

    public static Authenticate with(AuthRequest credentials) {
        return new Authenticate(credentials);
    }

    @Override
    public void performAs(Actor actor) {
        Response response = CallTheApi.as(actor).authApi().createToken(credentials);
        actor.recordResponse(response);
        String token = issuedToken(response);
        tokenIssued = token != null;
        if (tokenIssued) {
            actor.can(UseAnAuthToken.withToken(token));
        }
    }

    @Override
    public boolean succeededFor(Actor actor) {
        return Performable.super.succeededFor(actor) && tokenIssued;
    }

    private static String issuedToken(Response response) {
        if (response.statusCode() / 100 != 2) {
            return null;
        }
        String token = response.jsonPath().getString(TOKEN);
        return token == null || token.isBlank() ? null : token;
    }

    @Override
    public String toString() {
        return "authenticate as '" + credentials.getUsername() + "'";
    }
}
