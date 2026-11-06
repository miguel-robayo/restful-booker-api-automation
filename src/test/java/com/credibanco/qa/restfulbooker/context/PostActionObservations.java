package com.credibanco.qa.restfulbooker.context;

import com.credibanco.qa.restfulbooker.reporting.AllureReporter;
import com.credibanco.qa.restfulbooker.screenplay.core.Actor;
import com.credibanco.qa.restfulbooker.screenplay.core.Question;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit observation phase between the action and its verification (ADR-22).
 *
 * <p>A "Cuando" step performs only the scenario's main action and <b>declares</b> here which evidence it needs
 * afterwards. The {@code @AfterStep} hook runs the declared observations right after that step, each one as its own
 * visible Allure step ("Post-action observation: ..."), and stores the results in the actor's memory. "Entonces"
 * steps then verify stored evidence only (ADR-13). Observations are {@link Question}s: reads, never writes, and
 * they never assert.
 *
 * <p>One instance per scenario (owned by {@link ScenarioContext}); not shared.
 */
public final class PostActionObservations {

    private final List<Observation<?>> declared = new ArrayList<>();

    public <T> void after(String description, Question<T> question, String memoryKey) {
        declared.add(new Observation<>(description, question, memoryKey));
    }

    /** Runs and clears the declared observations, in declaration order. */
    public void runDeclared(Actor actor) {
        List<Observation<?>> toRun = List.copyOf(declared);
        declared.clear();
        toRun.forEach(observation -> observation.runFor(actor));
    }

    private record Observation<T>(String description, Question<T> question, String memoryKey) {

        void runFor(Actor actor) {
            AllureReporter.step("Post-action observation: " + description,
                    () -> actor.remember(memoryKey, actor.asksFor(question)));
        }
    }
}
