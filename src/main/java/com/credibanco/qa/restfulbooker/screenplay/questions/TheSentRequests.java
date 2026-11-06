package com.credibanco.qa.restfulbooker.screenplay.questions;

import com.credibanco.qa.restfulbooker.http.SentRequest;
import com.credibanco.qa.restfulbooker.screenplay.abilities.RecordSentRequests;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Requests the actor actually sent, with token fingerprints. Reads memory only: no HTTP call, so these questions are
 * safe in "Entonces" steps (ADR-13). They keep the HTTP details (methods and paths) out of the step definitions.
 */
public final class TheSentRequests {

    private static final String AUTH_PATH = "/auth";
    private static final String BOOKING_PATH = "/booking/";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";

    private TheSentRequests() {
    }

    public static Question<List<SentRequest>> all() {
        return Question.about("the requests sent so far", TheSentRequests::sentBy);
    }

    /** Fingerprint of the session token carried by the first modification of the booking, if any. */
    public static Question<Optional<String>> sessionUsedToModify(long bookingId) {
        return Question.about("the session used to modify booking " + bookingId,
                actor -> sessionUsedBy(actor, sent(PUT, BOOKING_PATH + bookingId)));
    }

    /** Fingerprint of the session token carried by the first cancellation of the booking, if any. */
    public static Question<Optional<String>> sessionUsedToCancel(long bookingId) {
        return Question.about("the session used to cancel booking " + bookingId,
                actor -> sessionUsedBy(actor, sent(DELETE, BOOKING_PATH + bookingId)));
    }

    /** How many times the actor signed in. */
    public static Question<Long> signIns() {
        return Question.about("the number of sign-ins",
                actor -> sentBy(actor).stream().filter(sent(POST, AUTH_PATH)).count());
    }

    private static Optional<String> sessionUsedBy(Actor actor, Predicate<SentRequest> operation) {
        return sentBy(actor).stream().filter(operation).findFirst().flatMap(SentRequest::tokenFingerprint);
    }

    private static Predicate<SentRequest> sent(String method, String path) {
        return request -> request.method().equals(method) && request.path().equals(path);
    }

    private static List<SentRequest> sentBy(Actor actor) {
        return RecordSentRequests.as(actor).sent();
    }
}
