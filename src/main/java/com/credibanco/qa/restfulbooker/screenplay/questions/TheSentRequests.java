package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.http.SentRequest;
import com.credibanco.qa.restfulbooker.screenplay.abilities.RecordSentRequests;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;

import java.util.List;

/**
 * Requests the actor actually sent, with token fingerprints. Reads memory only: no HTTP call.
 */
public final class TheSentRequests {

    private TheSentRequests() {
    }

    public static Question<List<SentRequest>> all() {
        return Question.about("the requests sent so far", actor -> RecordSentRequests.as(actor).sent());
    }
}
