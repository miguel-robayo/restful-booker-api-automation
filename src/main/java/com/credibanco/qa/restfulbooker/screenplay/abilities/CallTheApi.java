package com.credibanco.qa.restfulbooker.screenplay.abilities;

import com.credibanco.qa.restfulbooker.api.AuthApi;
import com.credibanco.qa.restfulbooker.api.BookingApi;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.screenplay.core.Ability;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import io.restassured.filter.Filter;

/**
 * Gives an actor API Objects built with the right spec. Specs are created per call, never cached. When the actor
 * can {@link RecordSentRequests}, every request it sends is recorded.
 */
public final class CallTheApi implements Ability {

    private static final Filter[] NO_FILTERS = new Filter[0];

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

    public AuthApi authApiFor(Actor actor) {
        return new AuthApi(specs.base(recorderOf(actor)));
    }

    /** Without token: creation and reads, which the API does not protect. */
    public BookingApi publicBookingApiFor(Actor actor) {
        return new BookingApi(specs.base(recorderOf(actor)));
    }

    /** With the actor's token if it holds one (valid or deliberately invalid), otherwise without token. */
    public BookingApi bookingApiFor(Actor actor) {
        return actor.hasAbilityTo(UseAnAuthToken.class)
                ? new BookingApi(specs.authenticated(UseAnAuthToken.as(actor).token(), recorderOf(actor)))
                : publicBookingApiFor(actor);
    }

    private static Filter[] recorderOf(Actor actor) {
        return actor.hasAbilityTo(RecordSentRequests.class)
                ? new Filter[]{RecordSentRequests.as(actor).filter()}
                : NO_FILTERS;
    }
}
