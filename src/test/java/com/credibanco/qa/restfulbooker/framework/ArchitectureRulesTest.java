package com.credibanco.qa.restfulbooker.framework;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layer rules of the architecture (ADR-02, ADR-03, ADR-04, §11), checked on the source code without extra
 * dependencies.
 */
@DisplayName("Architecture rules")
class ArchitectureRulesTest {

    private static final Path MAIN = Paths.get("src", "main", "java", "com", "credibanco", "qa", "restfulbooker");
    private static final Path TEST = Paths.get("src", "test", "java", "com", "credibanco", "qa", "restfulbooker");

    private static final Pattern GLOBAL_REST_ASSURED_STATE = Pattern.compile(
            "RestAssured\\.(baseURI|basePath|port|rootPath|urlEncodingEnabled|authentication|config|"
                    + "requestSpecification|responseSpecification|defaultParser|filters\\s*\\(|reset\\s*\\(|"
                    + "enableLoggingOfRequestAndResponseIfValidationFails\\s*\\()");
    private static final Pattern ASSERTIONS = Pattern.compile(
            "import\\s+(static\\s+)?(org\\.junit|org\\.hamcrest|org\\.assertj)");
    private static final Pattern HTTP_ENTRY_POINT = Pattern.compile("RestAssured\\.given|\\bgiven\\s*\\(");
    /** Steps may read a stored {@code Response}, but never start a request or touch API Objects or specs. */
    private static final Pattern HTTP_IN_STEPS = Pattern.compile(
            "import\\s+(static\\s+)?(io\\.restassured\\.RestAssured|io\\.restassured\\.specification|"
                    + "io\\.restassured\\.builder|com\\.credibanco\\.qa\\.restfulbooker\\.api)|\\bgiven\\s*\\(");
    private static final Pattern SLEEP = Pattern.compile("Thread\\s*\\.\\s*sleep|TimeUnit\\.[A-Z]+\\.sleep");
    private static final Path QUESTIONS = MAIN.resolve(Paths.get("screenplay", "questions"));
    /** A Question is an HTTP read when it uses the API ability, directly or through another HTTP Question. */
    private static final Pattern USES_THE_API = Pattern.compile("\\bCallTheApi\\b");
    private static final Pattern STEP_ANNOTATION = Pattern.compile("@(Dado|Cuando|Entonces|Y|Pero)\\s*\\(");
    private static final Pattern PRIVATE_METHOD = Pattern.compile(
            "private\\s+(?:static\\s+)?[\\w<>,?\\[\\]\\s]+?\\s+(\\w+)\\s*\\(");
    /** Questions that only read the actor's memory; used as a sanity check of the HTTP detection. */
    private static final Set<String> MEMORY_ONLY_QUESTIONS = Set.of("TheLastResponse", "TheSentRequests", "BookingsPricedAbove");
    private static final String STEPS_PACKAGE = "com.credibanco.qa.restfulbooker.steps";
    private static final String VERIFY_PREFIX = "verify";

    @Test
    @DisplayName("nobody writes static RestAssured state")
    void noGlobalRestAssuredState() {
        assertEquals(List.of(), offending(MAIN, GLOBAL_REST_ASSURED_STATE));
    }

    @Test
    @DisplayName("the framework (API Objects, Screenplay, HTTP) contains no assertions")
    void noAssertionsInFramework() {
        assertEquals(List.of(), offending(MAIN, ASSERTIONS));
    }

    @Test
    @DisplayName("only API Objects start HTTP requests")
    void onlyApiObjectsStartRequests() {
        List<String> offenders = offending(MAIN, HTTP_ENTRY_POINT).stream()
                .filter(file -> !file.replace('\\', '/').contains("/api/"))
                .toList();

        assertEquals(List.of(), offenders);
    }

    @Test
    @DisplayName("step definitions never use Rest Assured or API Objects directly")
    void stepsAreThinAdapters() {
        assertEquals(List.of(), offending(TEST.resolve("steps"), HTTP_IN_STEPS));
    }

    @Test
    @DisplayName("step methods are named by phase: \"verify…\" for \"Entonces\", an imperative verb otherwise (C-5)")
    void stepMethodsAreNamedByPhase() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> steps : stepClasses()) {
            for (Method method : steps.getDeclaredMethods()) {
                boolean verifies = method.getName().startsWith(VERIFY_PREFIX);
                if (method.isAnnotationPresent(Entonces.class) && !verifies) {
                    offenders.add(steps.getSimpleName() + "." + method.getName() + ": \"Entonces\" must start with "
                            + VERIFY_PREFIX);
                }
                if ((method.isAnnotationPresent(Dado.class) || method.isAnnotationPresent(Cuando.class)) && verifies) {
                    offenders.add(steps.getSimpleName() + "." + method.getName()
                            + ": \"Dado\"/\"Cuando\" must use an imperative verb, not " + VERIFY_PREFIX);
                }
            }
        }

        assertEquals(List.of(), offenders);
    }

    @Test
    @DisplayName("no artificial waits (Thread.sleep) anywhere")
    void noArtificialWaits() {
        List<String> offenders = Stream.concat(offending(MAIN, SLEEP).stream(), offending(TEST, SLEEP).stream())
                .filter(file -> !file.endsWith("ArchitectureRulesTest.java"))
                .toList();

        assertEquals(List.of(), offenders);
    }

    @Test
    @DisplayName("\"Entonces\" steps make no HTTP calls: they only check stored results")
    void thenStepsMakeNoHttpCalls() {
        Pattern httpAction = httpAction(httpQuestions());
        assertEquals(List.of(), javaFiles(TEST.resolve("steps")).stream()
                .flatMap(file -> thenStepsWithHttp(file.getFileName().toString(), read(file), httpAction).stream())
                .toList());
    }

    @Test
    @DisplayName("HTTP Questions are derived from the code, not from a fixed list")
    void httpQuestionsAreDerivedFromTheCode() {
        Set<String> httpQuestions = httpQuestions();

        assertTrue(httpQuestions.containsAll(Set.of("TheBooking", "BookingIds", "BookingDetails", "HighestBookingId")),
                "detected HTTP questions: " + httpQuestions);
        assertEquals(Set.of(), httpQuestions.stream().filter(MEMORY_ONLY_QUESTIONS::contains).collect(Collectors.toSet()),
                "memory-only questions must not be classified as HTTP");
    }

    @Test
    @DisplayName("an \"Entonces\" step that reaches HTTP through a private helper is detected")
    void thenStepUsingAnHttpHelperIsDetected() {
        Pattern httpAction = httpAction(Set.of("TheBooking"));
        String steps = """
                @Entonces("la reserva conserva sus datos")
                public void verifyKept() {
                    assertEquals(expected, miguel.asksFor(currentState()));
                }

                @Entonces("la reserva sigue registrada")
                public void verifyStillRegistered() {
                    assertEquals(expected, miguel.recall(STATE));
                }

                private Question<Optional<Booking>> currentState() {
                    return wrapped();
                }

                private Question<Optional<Booking>> wrapped() {
                    return TheBooking.withId(ownBookingId());
                }
                """;

        assertEquals(List.of("Sample.java: @Entonces(\"la reserva conserva sus datos\")"),
                thenStepsWithHttp("Sample.java", steps, httpAction));
    }

    /** Each "Cuando" method, from its annotation to the closing brace of its body, contains exactly one HTTP action. */
    @Test
    @DisplayName("each \"Cuando\" step performs exactly one action; further reads are declared post-action observations")
    void whenStepsPerformExactlyOneAction() {
        Pattern httpAction = httpAction(httpQuestions());
        List<String> offenders = new ArrayList<>();
        for (Path file : javaFiles(TEST.resolve("steps"))) {
            String source = read(file);
            Matcher annotation = STEP_ANNOTATION.matcher(source);
            while (annotation.find()) {
                if (annotation.group(1).equals("Cuando")) {
                    String method = source.substring(annotation.start(), methodEnd(source, annotation.end()));
                    long actions = httpAction.matcher(method).results().count();
                    if (actions != 1) {
                        offenders.add(file.getFileName() + ": " + method.lines().findFirst().orElse("") + " -> " + actions);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders);
    }

    /**
     * HTTP happens only through Tasks or HTTP Questions. HTTP Questions are those under {@code screenplay/questions}
     * that use {@code CallTheApi}, plus, transitively, those that compose another HTTP Question.
     */
    private static Set<String> httpQuestions() {
        Map<String, String> sources = javaFiles(QUESTIONS).stream().collect(Collectors.toMap(
                file -> file.getFileName().toString().replace(".java", ""), ArchitectureRulesTest::read));
        Set<String> http = sources.entrySet().stream()
                .filter(question -> USES_THE_API.matcher(question.getValue()).find())
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
        boolean grew = !http.isEmpty();
        while (grew) {
            Pattern usesHttp = referencesTo(http);
            grew = http.addAll(sources.entrySet().stream()
                    .filter(question -> usesHttp.matcher(question.getValue()).find())
                    .map(Map.Entry::getKey)
                    .toList());
        }
        return http;
    }

    private static Pattern httpAction(Set<String> httpQuestions) {
        String questions = httpQuestions.stream().map(name -> "|" + Pattern.quote(name) + "\\.").collect(Collectors.joining());
        return Pattern.compile("\\b(attemptsTo|wasAbleTo" + questions + ")");
    }

    private static Pattern referencesTo(Set<String> classNames) {
        return Pattern.compile("\\b(" + classNames.stream().map(Pattern::quote).collect(Collectors.joining("|")) + ")\\.");
    }

    /** Each "Entonces" method, from its annotation to its closing brace, that reaches HTTP directly or via a helper. */
    private static List<String> thenStepsWithHttp(String fileName, String source, Pattern httpAction) {
        Set<String> helpers = httpHelpers(source, httpAction);
        Pattern helperCall = helpers.isEmpty() ? null
                : Pattern.compile("\\b(" + String.join("|", helpers) + ")\\s*\\(");
        Matcher annotation = STEP_ANNOTATION.matcher(source);
        List<String> offenders = new ArrayList<>();
        while (annotation.find()) {
            if (!annotation.group(1).equals("Entonces")) {
                continue;
            }
            String method = source.substring(annotation.start(), methodEnd(source, annotation.end()));
            if (httpAction.matcher(method).find() || (helperCall != null && helperCall.matcher(method).find())) {
                offenders.add(fileName + ": " + method.lines().findFirst().orElse(""));
            }
        }
        return offenders;
    }

    /** Private methods of a step class that reach HTTP, directly or through another such helper. */
    private static Set<String> httpHelpers(String source, Pattern httpAction) {
        Map<String, String> bodies = new LinkedHashMap<>();
        Matcher method = PRIVATE_METHOD.matcher(source);
        while (method.find()) {
            int open = source.indexOf('{', method.end());
            if (open >= 0) {
                bodies.put(method.group(1), source.substring(open, blockEnd(source, open)));
            }
        }
        Set<String> http = new LinkedHashSet<>();
        boolean grew = true;
        while (grew) {
            Pattern helperCall = http.isEmpty() ? null : Pattern.compile("\\b(" + String.join("|", http) + ")\\s*\\(");
            grew = http.addAll(bodies.entrySet().stream()
                    .filter(body -> httpAction.matcher(body.getValue()).find()
                            || (helperCall != null && helperCall.matcher(body.getValue()).find()))
                    .map(Map.Entry::getKey)
                    .toList());
        }
        return http;
    }

    /** Brace matching starts at the method body, after the signature: step texts such as "{int}" contain braces. */
    private static int methodEnd(String source, int from) {
        int signature = source.indexOf("public ", from);
        return blockEnd(source, source.indexOf('{', signature < 0 ? from : signature));
    }

    /** Index just after the brace that closes the block opened at {@code open}. */
    private static int blockEnd(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i + 1;
            }
        }
        return source.length();
    }

    private static List<Class<?>> stepClasses() {
        List<Class<?>> classes = new ArrayList<>();
        for (Path file : javaFiles(TEST.resolve("steps"))) {
            String simpleName = file.getFileName().toString().replace(".java", "");
            try {
                classes.add(Class.forName(STEPS_PACKAGE + "." + simpleName));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Step class not compiled: " + simpleName, e);
            }
        }
        assertTrue(!classes.isEmpty(), "no step classes found under " + TEST.resolve("steps"));
        return classes;
    }

    private static List<Path> javaFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> offending(Path root, Pattern pattern) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .filter(file -> pattern.matcher(read(file)).find())
                    .map(Path::toString)
                    .sorted()
                    .toList();
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
