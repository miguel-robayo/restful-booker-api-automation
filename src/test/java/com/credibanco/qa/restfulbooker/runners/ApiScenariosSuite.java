package com.credibanco.qa.restfulbooker.runners;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Runs every feature under {@code src/test/resources/features} with the Cucumber engine on the JUnit Platform.
 * Tag filtering ({@code not @known-defect} vs. {@code @known-defect}) comes from Surefire (D-21).
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
public class ApiScenariosSuite {
}
