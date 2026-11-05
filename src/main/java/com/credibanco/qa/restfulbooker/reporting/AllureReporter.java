package com.credibanco.qa.restfulbooker.reporting;

import io.qameta.allure.Allure;

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
}
