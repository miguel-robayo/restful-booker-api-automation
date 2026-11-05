package com.credibanco.qa.restfulbooker.context;

import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.data.BookingDataFactory;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.screenplay.abilities.CallTheApi;
import com.credibanco.qa.restfulbooker.screenplay.abilities.RecordSentRequests;
import com.credibanco.qa.restfulbooker.screenplay.abilities.TrackCreatedBookings;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Everything one scenario owns: Miguel (the actor), the test data factory, the specs and the post-action
 * observations. PicoContainer creates a new instance per scenario (ADR-15); there are no static fields, so parallel
 * scenarios never share state.
 */
public class ScenarioContext {

    private static final String SEED_PROPERTY = "data.seed";
    private static final String ACTOR_NAME = "Miguel";

    private final EnvironmentConfig config;
    private final SpecFactory specs;
    private final BookingDataFactory testData;
    private final Actor miguel;
    private final PostActionObservations observations = new PostActionObservations();

    public ScenarioContext() {
        this.config = EnvironmentConfig.current();
        this.specs = new SpecFactory(config);
        this.testData = BookingDataFactory.withSeed(Long.getLong(SEED_PROPERTY, ThreadLocalRandom.current().nextLong()));
        this.miguel = Actor.named(ACTOR_NAME)
                .can(CallTheApi.using(specs))
                .can(TrackCreatedBookings.forThisScenario())
                .can(RecordSentRequests.forThisScenario());
    }

    public EnvironmentConfig config() {
        return config;
    }

    public SpecFactory specs() {
        return specs;
    }

    public BookingDataFactory testData() {
        return testData;
    }

    public Actor miguel() {
        return miguel;
    }

    public PostActionObservations observations() {
        return observations;
    }
}
