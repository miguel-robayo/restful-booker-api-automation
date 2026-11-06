package com.credibanco.qa.restfulbooker.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resolves {@link EnvironmentConfig} with this precedence (highest first):
 * {@code -Dkey} &gt; {@code RB_KEY} environment variable &gt; {@code config/<env>.properties} &gt;
 * {@code config/default.properties}. File values may reference environment variables as {@code ${VAR}}.
 *
 * <p>All sources are injected so the resolution can be tested without touching global state.
 */
public final class ConfigResolver {

    static final String DEFAULT_ENVIRONMENT = "qa";

    private static final String DEFAULT_FILE = "default";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_]+)}");

    private final Map<String, String> systemProperties;
    private final Map<String, String> environmentVariables;
    private final Function<String, Optional<Properties>> configFileLoader;

    public ConfigResolver(Map<String, String> systemProperties, Map<String, String> environmentVariables,
                          Function<String, Optional<Properties>> configFileLoader) {
        this.systemProperties = Map.copyOf(systemProperties);
        this.environmentVariables = Map.copyOf(environmentVariables);
        this.configFileLoader = configFileLoader;
    }

    public static ConfigResolver fromRuntime() {
        Map<String, String> systemProperties = System.getProperties().stringPropertyNames().stream()
                .collect(Collectors.toMap(name -> name, System::getProperty));
        return new ConfigResolver(systemProperties, System.getenv(), ConfigResolver::loadFromClasspath);
    }

    public EnvironmentConfig resolve() {
        Properties defaults = configFileLoader.apply(DEFAULT_FILE).orElseThrow(() ->
                new ConfigurationException("Missing config/" + DEFAULT_FILE + ".properties on the test classpath."));
        String environment = override(ConfigKeys.ENVIRONMENT).orElse(DEFAULT_ENVIRONMENT);
        Properties environmentFile = loadEnvironment(environment, defaults);

        Properties files = new Properties();
        files.putAll(defaults);
        files.putAll(environmentFile);

        return new EnvironmentConfig(
                environment,
                httpsUrl(required(ConfigKeys.BASE_URL, files)),
                value(ConfigKeys.USERNAME, files).orElse(null),
                value(ConfigKeys.PASSWORD, files).orElse(null),
                number(ConfigKeys.SLA_BOOKING_GET_MS, files),
                Math.toIntExact(number(ConfigKeys.QUERY_PRICE_THRESHOLD, files)),
                number(ConfigKeys.NOT_FOUND_BOOKING_ID, files),
                Math.toIntExact(number(ConfigKeys.HTTP_TIMEOUT_MS, files)));
    }

    private Properties loadEnvironment(String environment, Properties defaults) {
        List<String> declared = Arrays.stream(defaults.getProperty(ConfigKeys.ENVIRONMENTS, "").split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        Optional<Properties> file = declared.contains(environment)
                ? configFileLoader.apply(environment)
                : Optional.empty();
        return file.orElseThrow(() -> new ConfigurationException("Unknown environment '" + environment
                + "'. Use -D" + ConfigKeys.ENVIRONMENT + "=<name> with one of: " + String.join(", ", declared) + "."));
    }

    /** A value set explicitly for this run: {@code -Dkey} first, then the {@code RB_KEY} environment variable. */
    private Optional<String> override(String key) {
        return nonBlank(systemProperties.get(key))
                .or(() -> nonBlank(environmentVariables.get(ConfigKeys.environmentVariableFor(key))));
    }

    /** Resolved value, or empty when it is missing, blank or references an undefined environment variable. */
    private Optional<String> value(String key, Properties files) {
        return override(key)
                .or(() -> nonBlank(files.getProperty(key)))
                .flatMap(this::expandPlaceholders);
    }

    private String required(String key, Properties files) {
        return value(key, files).orElseThrow(() -> new ConfigurationException("Missing configuration '" + key
                + "'. Define it in config/<env>.properties, as " + ConfigKeys.environmentVariableFor(key)
                + " or with -D" + key + "=<value>."));
    }

    private long number(String key, Properties files) {
        String raw = required(key, files);
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid value '" + raw + "' for '" + key + "': expected a whole number.", e);
        }
    }

    private Optional<String> expandPlaceholders(String raw) {
        Matcher matcher = PLACEHOLDER.matcher(raw);
        StringBuilder expanded = new StringBuilder();
        while (matcher.find()) {
            String variable = environmentVariables.get(matcher.group(1));
            if (variable == null || variable.isBlank()) {
                return Optional.empty();
            }
            matcher.appendReplacement(expanded, Matcher.quoteReplacement(variable));
        }
        matcher.appendTail(expanded);
        return nonBlank(expanded.toString());
    }

    private static URI httpsUrl(String raw) {
        URI uri;
        try {
            uri = new URI(raw.trim());
        } catch (URISyntaxException e) {
            throw new ConfigurationException("Invalid '" + ConfigKeys.BASE_URL + "': " + raw, e);
        }
        if (!"https".equals(uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT))
                || uri.getHost() == null) {
            throw new ConfigurationException("'" + ConfigKeys.BASE_URL + "' must be an https URL, but was: " + raw);
        }
        return uri;
    }

    private static Optional<String> nonBlank(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    /** Loads {@code config/<name>.properties} from the classpath. */
    public static Optional<Properties> loadFromClasspath(String name) {
        String resource = "config/" + name + ".properties";
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return Optional.empty();
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return Optional.of(properties);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read " + resource + " from the classpath.", e);
        }
    }
}
