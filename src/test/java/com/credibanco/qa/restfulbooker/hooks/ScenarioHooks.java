package com.credibanco.qa.restfulbooker.hooks;

import com.credibanco.qa.restfulbooker.config.EnvironmentConfig;
import com.credibanco.qa.restfulbooker.context.ScenarioContext;
import com.credibanco.qa.restfulbooker.http.JsonMapper;
import com.credibanco.qa.restfulbooker.http.SpecFactory;
import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import com.credibanco.qa.restfulbooker.screenplay.abilities.TrackCreatedBookings;
import com.credibanco.qa.restfulbooker.support.BookingCleaner;
import com.credibanco.qa.restfulbooker.support.CleanupReport;
import com.credibanco.qa.restfulbooker.support.CleanupResult;
import com.credibanco.qa.restfulbooker.support.EnvironmentHealthCheck;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.Scenario;

/**
 * The only lifecycle mechanism of the scenarios (ADR-12): environment check once, seed per scenario, post-action
 * observations right after the action (ADR-22), cleanup after every scenario (even a failed one) and a consolidated
 * cleanup report at the end. Logic lives in {@code support} and {@code context}.
 */
public class ScenarioHooks {

    private final ScenarioContext context;

    public ScenarioHooks(ScenarioContext context) {
        this.context = context;
    }

    @BeforeAll
    public static void verifyEnvironment() {
        CleanupReport.reset();
        EnvironmentHealthCheck.verify(new SpecFactory(EnvironmentConfig.current()));
    }

    @Before
    public void recordDataSeed() {
        long seed = context.testData().seed();
        AllureReporter.attach("Data seed", "data.seed=" + seed + "\nReplay this scenario's data with -Ddata.seed=" + seed);
    }

    /** Runs the evidence reads declared by the "Cuando" step; nothing is declared after other steps. */
    @AfterStep
    public void runPostActionObservations(Scenario scenario) {
        if (!scenario.isFailed()) {
            context.observations().runDeclared(context.miguel());
        }
    }

    @After
    public void cleanUpCreatedBookings(Scenario scenario) {
        CleanupResult result = new BookingCleaner(context.specs())
                .clean(TrackCreatedBookings.as(context.miguel()).created());
        CleanupReport.record(scenario.getName(), result);
        if (!result.pending().isEmpty()) {
            AllureReporter.brokenStep("Data cleanup pending", "cleanup-pending.json", pendingAsJson(result));
        }
    }

    @AfterAll
    public static void consolidateCleanupReport() {
        CleanupReport.consolidate();
    }

    private static String pendingAsJson(CleanupResult result) {
        try {
            return JsonMapper.instance().writerWithDefaultPrettyPrinter().writeValueAsString(result.pending());
        } catch (JsonProcessingException e) {
            return result.pending().toString();
        }
    }
}
