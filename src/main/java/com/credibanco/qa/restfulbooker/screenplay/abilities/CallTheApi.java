package com.credibanco.qa.restfulbooker.screenplay.abilities;

import com.credibanco.qa.restfulbooker.api.AuthApi;
import com.credibanco.qa.restfulbooker.api.BookingApi;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.screenplay.core.Ability;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;

/**
 * Gives an actor API Objects built with the right spec: authenticated when the actor holds
 * {@link UseAnAuthToken}, unauthenticated otherwise. Specs are created per call, never cached.
 */
public final class CallTheApi implements Ability {

    private final SpecFactory specs;

    private CallTheApi(SpecFactory specs) {
        this.specs = specs;
    }

    public static CallTheApi using(SpecFactory specs) {
        return new CallTheApi(specs);
    }

    public static CallTheApi as(Actor actor) {
        return actor.abilityTo(CallTheApi.class);
    }

    public AuthApi authApi() {
        return new AuthApi(specs.base());
    }

    /** Unauthenticated access, for reads and for negative scenarios without a token. */
    public BookingApi bookingApi() {
        return new BookingApi(specs.base());
    }

    /** Access with the actor's token if it has one (valid or deliberately invalid), otherwise unauthenticated. */
    public BookingApi bookingApiFor(Actor actor) {
        return actor.hasAbilityTo(UseAnAuthToken.class)
                ? new BookingApi(specs.authenticated(UseAnAuthToken.as(actor).token()))
                : bookingApi();
    }
}
