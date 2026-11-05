package com.credibanco.qa.restfulbooker.screenplay.abilities;

import com.credibanco.qa.restfulbooker.http.SentRequest;
import com.credibanco.qa.restfulbooker.http.SentRequestFilter;
import com.credibanco.qa.restfulbooker.screenplay.core.Ability;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps every request the actor actually sent, with the fingerprint of the token it carried, so a scenario can prove
 * which credential each operation used without revealing it. Owned by one actor; not shared.
 */
public final class RecordSentRequests implements Ability {

    private final List<SentRequest> sent = new ArrayList<>();

    public static RecordSentRequests forThisScenario() {
        return new RecordSentRequests();
    }

    public static RecordSentRequests as(Actor actor) {
        return actor.abilityTo(RecordSentRequests.class);
    }

    SentRequestFilter filter() {
        return new SentRequestFilter(sent::add);
    }

    public List<SentRequest> sent() {
        return List.copyOf(sent);
    }
}
