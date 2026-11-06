package com.credibanco.qa.restfulbooker.runners;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Focused runner for scenario 2 (booking creation): runs only {@code features/booking_creation.feature}.
 * <p>
 * Usage: {@code mvn test -Dtest=BookingCreationTests -Denv=qa}. Its name does not match the Surefire includes
 * ({@code *Test}, {@code *Suite}), so the main job does not run these scenarios twice; they are already part of
 * {@link ApiScenariosSuite}. The tag filter ({@code not @known-defect}) still comes from Surefire (D-21).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/booking_creation.feature")
public class BookingCreationTests {
}
