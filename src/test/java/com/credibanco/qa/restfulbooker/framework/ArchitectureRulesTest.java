package com.credibanco.qa.restfulbooker.framework;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    /** HTTP happens only through Tasks or HTTP Questions; neither may appear in an "Entonces" step. */
    private static final Pattern HTTP_ACTION = Pattern.compile(
            "\\b(attemptsTo|wasAbleTo|TheBooking\\.|BookingIds\\.|BookingDetails\\.|HighestBookingId\\.)");
    private static final Pattern STEP_ANNOTATION = Pattern.compile("@(Dado|Cuando|Entonces|Y|Pero)\\s*\\(");

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
        assertEquals(List.of(), javaFiles(TEST.resolve("steps")).stream()
                .flatMap(file -> thenBlocksWithHttp(file).stream())
                .toList());
    }

    /** Each "Entonces" method: from its annotation to the closing brace of its body. */
    @Test
    @DisplayName("each \"Cuando\" step performs exactly one action; further reads are declared post-action observations")
    void whenStepsPerformExactlyOneAction() {
        List<String> offenders = new java.util.ArrayList<>();
        for (Path file : javaFiles(TEST.resolve("steps"))) {
            String source = read(file);
            java.util.regex.Matcher annotation = STEP_ANNOTATION.matcher(source);
            while (annotation.find()) {
                if (annotation.group(1).equals("Cuando")) {
                    String method = source.substring(annotation.start(), methodEnd(source, annotation.end()));
                    long actions = HTTP_ACTION.matcher(method).results().count();
                    if (actions != 1) {
                        offenders.add(file.getFileName() + ": " + method.lines().findFirst().orElse("") + " -> " + actions);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders);
    }

    private static List<String> thenBlocksWithHttp(Path file) {
        String source = read(file);
        java.util.regex.Matcher annotation = STEP_ANNOTATION.matcher(source);
        List<String> offenders = new java.util.ArrayList<>();
        while (annotation.find()) {
            if (!annotation.group(1).equals("Entonces")) {
                continue;
            }
            String method = source.substring(annotation.start(), methodEnd(source, annotation.end()));
            if (HTTP_ACTION.matcher(method).find()) {
                offenders.add(file.getFileName() + ": " + method.lines().findFirst().orElse(""));
            }
        }
        return offenders;
    }

    /** Brace matching starts at the method body, after the signature: step texts such as "{int}" contain braces. */
    private static int methodEnd(String source, int from) {
        int signature = source.indexOf("public ", from);
        int open = source.indexOf('{', signature < 0 ? from : signature);
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
