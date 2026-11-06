package com.credibanco.qa.restfulbooker.support;

import com.credibanco.qa.restfulbooker.http.JsonMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Cleanup evidence of a run (D-08). Each scenario writes its own uniquely named file, so parallel scenarios never
 * share state; the consolidation runs once, after all scenarios. Files live next to the job's Allure results
 * ({@code target/cleanup/<results-dir>/}), so the main and the known-defects jobs stay separate.
 */
public final class CleanupReport {

    private static final System.Logger LOG = System.getLogger(CleanupReport.class.getName());
    private static final String RESULTS_PROPERTY = "allure.results.directory";
    private static final String DEFAULT_RESULTS = "target/allure-results";
    private static final String CONSOLIDATED = "cleanup-pending.json";
    private static final String ENVIRONMENT_FILE = "environment.properties";
    private static final TypeReference<List<CleanupResult.Outcome>> OUTCOMES = new TypeReference<>() {
    };

    private CleanupReport() {
    }

    /** Removes evidence of a previous run of the same job. */
    public static void reset() {
        Path directory = directory();
        if (Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                files.forEach(CleanupReport::delete);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Writes the outcomes of one scenario to its own file. */
    public static void record(String scenario, CleanupResult result) {
        if (result.outcomes().isEmpty()) {
            return;
        }
        Path file = directory().resolve(UUID.randomUUID() + ".json");
        write(file, Map.of("scenario", scenario, "outcomes", result.outcomes()));
    }

    /** Consolidates every scenario file; returns how many bookings are still pending. */
    public static int consolidate() {
        List<CleanupResult.Outcome> pending = readAll().stream()
                .filter(outcome -> outcome.status() == CleanupResult.Status.PENDING)
                .toList();
        Path report = directory().resolve(CONSOLIDATED);
        write(report, pending);
        writeEnvironment(pending.size(), report);
        if (!pending.isEmpty()) {
            LOG.log(System.Logger.Level.WARNING, "{0} booking(s) could not be cleaned up; see {1}", pending.size(), report);
        }
        return pending.size();
    }

    private static List<CleanupResult.Outcome> readAll() {
        Path directory = directory();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> !file.getFileName().toString().equals(CONSOLIDATED))
                    .flatMap(file -> outcomesOf(file).stream())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<CleanupResult.Outcome> outcomesOf(Path file) {
        try {
            return JsonMapper.instance().convertValue(JsonMapper.instance().readTree(file.toFile()).get("outcomes"), OUTCOMES);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Adds the cleanup status to Allure's Environment panel. */
    private static void writeEnvironment(int pendingCount, Path report) {
        Path environment = resultsDirectory().resolve(ENVIRONMENT_FILE);
        String content = "cleanup.pending.count=" + pendingCount + "\n"
                + "cleanup.pending.report=" + report.toString().replace('\\', '/') + "\n";
        try {
            Files.createDirectories(environment.getParent());
            Files.writeString(environment, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, Object content) {
        try {
            Files.createDirectories(file.getParent());
            JsonMapper.instance().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path resultsDirectory() {
        return Paths.get(System.getProperty(RESULTS_PROPERTY, DEFAULT_RESULTS));
    }

    private static Path directory() {
        Path results = resultsDirectory().toAbsolutePath().normalize();
        return results.getParent().resolve("cleanup").resolve(results.getFileName());
    }
}
