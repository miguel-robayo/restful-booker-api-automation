package com.credibanco.qa.restfulbooker.framework;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * BDD writing conventions of the feature files (C-6), checked without extra dependencies: business language, one
 * action per scenario, Miguel introduced only in the first "Dado", and traceability tags on every scenario.
 */
@DisplayName("Feature conventions")
class FeatureConventionsTest {

    private static final Path FEATURES = Paths.get("src", "test", "resources", "features");
    private static final String ACTOR = "Miguel";

    /** Implementation terms that must stay in Java and technical documentation, never in Gherkin. */
    private static final Pattern TECHNICAL_TERMS = Pattern.compile("(?i)(?<![\\p{L}\\d])("
            + "api|rest|endpoint|http|token|cookie|json|schema|esquema|gpath|builder|sha-?256|rest assured|"
            + "seriali[zs]\\w*|deseriali[zs]\\w*|booking|content-type|charset|cabecera|header|sla|payload|"
            + "[1-5]\\d\\d)(?![\\p{L}\\d])");
    private static final Pattern STEP = Pattern.compile("^\\s*(Dado|Cuando|Entonces|Y|Pero)\\s+(.*)$");
    private static final Pattern SCENARIO = Pattern.compile("^\\s*(Escenario|Esquema del escenario):\\s*(.*)$");
    private static final Pattern FEATURE = Pattern.compile("^\\s*Característica:\\s*(.*)$");

    @Test
    @DisplayName("there are feature files to check")
    void featuresExist() {
        assertFalse(scenarios().isEmpty(), "no scenarios found under " + FEATURES);
    }

    @Test
    @DisplayName("every scenario has exactly one action (a single \"Cuando\", no \"Y\" after it)")
    void oneActionPerScenario() {
        List<String> offenders = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            long actions = scenario.effectiveKeywords().stream().filter("Cuando"::equals).count();
            if (actions != 1) {
                offenders.add(scenario.title() + " -> " + actions + " actions");
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    @DisplayName("Miguel is introduced in the first \"Dado\" and not repeated")
    void actorIntroducedOnce() {
        List<String> offenders = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            List<String> steps = scenario.steps();
            boolean introducedFirst = scenario.keywords().get(0).equals("Dado") && steps.get(0).startsWith("que " + ACTOR);
            long mentions = steps.stream().filter(step -> step.contains(ACTOR)).count();
            if (!introducedFirst || mentions != 1) {
                offenders.add(scenario.title());
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    @DisplayName("features, titles, steps and comments use business language only")
    void businessLanguageOnly() {
        List<String> offenders = new ArrayList<>();
        for (Path file : featureFiles()) {
            for (String line : read(file).lines().toList()) {
                String text = line.trim();
                boolean gherkinText = FEATURE.matcher(text).matches() || SCENARIO.matcher(text).matches()
                        || STEP.matcher(text).matches() || text.startsWith("#");
                if (gherkinText && !text.startsWith("# language:")) {
                    Matcher term = TECHNICAL_TERMS.matcher(text);
                    if (term.find()) {
                        offenders.add(file.getFileName() + ": '" + term.group() + "' in: " + text);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders);
    }

    @Test
    @DisplayName("every scenario keeps its traceability tags (@TC-* and @esc-*)")
    void traceabilityTags() {
        List<String> offenders = scenarios().stream()
                .filter(scenario -> !scenario.tags().matches(".*@TC-[A-Z]+-\\d{2}\\b.*") || !scenario.tags().matches(".*@esc-\\d\\b.*"))
                .map(Scenario::title)
                .toList();
        assertEquals(List.of(), offenders);
    }

    // ---------- parsing ----------

    private record Scenario(String title, String tags, List<String> keywords, List<String> steps) {

        /** "Y"/"Pero" take the meaning of the previous keyword, as in Gherkin. */
        List<String> effectiveKeywords() {
            List<String> effective = new ArrayList<>();
            String current = null;
            for (String keyword : keywords) {
                current = keyword.equals("Y") || keyword.equals("Pero") ? current : keyword;
                effective.add(current);
            }
            return effective;
        }
    }

    private static List<Scenario> scenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        for (Path file : featureFiles()) {
            String tags = "";
            Scenario current = null;
            for (String line : read(file).lines().toList()) {
                String text = line.trim();
                if (text.startsWith("@")) {
                    tags = text;
                }
                Matcher scenario = SCENARIO.matcher(text);
                if (scenario.matches()) {
                    current = new Scenario(file.getFileName() + " / " + scenario.group(2), tags, new ArrayList<>(), new ArrayList<>());
                    scenarios.add(current);
                    continue;
                }
                Matcher step = STEP.matcher(text);
                if (current != null && step.matches()) {
                    current.keywords().add(step.group(1));
                    current.steps().add(step.group(2));
                }
            }
        }
        return scenarios;
    }

    private static List<Path> featureFiles() {
        try (Stream<Path> files = Files.list(FEATURES)) {
            return files.filter(file -> file.toString().endsWith(".feature")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
