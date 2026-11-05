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
    private static final Pattern HTTP_IN_STEPS = Pattern.compile(
            "import\\s+(static\\s+)?(io\\.restassured|com\\.credibanco\\.qa\\.restfulbooker\\.api)");

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
