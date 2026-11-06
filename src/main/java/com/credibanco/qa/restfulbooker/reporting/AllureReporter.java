package com.credibanco.qa.restfulbooker.reporting;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Allure steps and attachments that only act while a test is being reported (ADR-06). Outside a Cucumber scenario
 * (e.g. technical JUnit tests) the work still runs, but nothing is sent to Allure, so no "no test is running" errors.
 */
public final class AllureReporter {

    private static final String TEXT = "text/plain";
    private static final String TXT = ".txt";

    private AllureReporter() {
    }

    public static boolean isReporting() {
        return Allure.getLifecycle().getCurrentTestCaseOrStep().isPresent();
    }

    public static <T> T step(String name, Supplier<T> work) {
        if (!isReporting()) {
            return work.get();
        }
        return Allure.step(name, work::get);
    }

    public static void step(String name, Runnable work) {
        step(name, () -> {
            work.run();
            return null;
        });
    }

    /** Attaches text that must already be free of secrets. */
    public static void attach(String name, String content) {
        if (isReporting()) {
            Allure.addAttachment(name, TEXT, content, TXT);
        }
    }

    // No parameter(...) helper on purpose: with allure-cucumber7-jvm 2.35.5, Allure.parameter throws
    // UnsupportedOperationException inside a step (immutable parameter list) and logs "test case not found" in hooks.
    // Values such as the data seed or the SLA measurement are reported as attachments instead.

    /**
     * A visible broken step with evidence that does <b>not</b> change the test's own status (D-08): used for
     * pending data cleanup, which must be visible without turning a functional result into a failure.
     */
    public static void brokenStep(String name, String attachmentName, String content) {
        if (!isReporting()) {
            return;
        }
        AllureLifecycle lifecycle = Allure.getLifecycle();
        String uuid = UUID.randomUUID().toString();
        lifecycle.startStep(uuid, new StepResult().setName(name).setStatus(Status.BROKEN));
        lifecycle.addAttachment(attachmentName, TEXT, TXT, content.getBytes(StandardCharsets.UTF_8));
        lifecycle.stopStep(uuid);
    }
}
