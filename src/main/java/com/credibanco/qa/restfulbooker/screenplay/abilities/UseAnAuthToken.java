package com.credibanco.qa.restfulbooker.screenplay.abilities;

import com.credibanco.qa.restfulbooker.http.SensitiveDataMasker;
import com.credibanco.qa.restfulbooker.screenplay.core.Ability;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;

import java.util.UUID;

/**
 * The actor's auth token. Immutable and owned by one actor, so tokens are never shared between parallel scenarios.
 */
public final class UseAnAuthToken implements Ability {

    private final String token;

    private UseAnAuthToken(String token) {
        this.token = token;
    }

    public static UseAnAuthToken withToken(String token) {
        return new UseAnAuthToken(token);
    }

    /** A random token the API never issued (TC-NEG-02); generated, never a literal. */
    public static UseAnAuthToken invalid() {
        return new UseAnAuthToken(UUID.randomUUID().toString().replace("-", ""));
    }

    public static UseAnAuthToken as(Actor actor) {
        return actor.abilityTo(UseAnAuthToken.class);
    }

    public String token() {
        return token;
    }

    @Override
    public String toString() {
        return "UseAnAuthToken[" + SensitiveDataMasker.MASK + "]";
    }
}
