package ar.edu.itba.paw.webapp.contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Minimal OpenAPI reader for contract tests (no YAML parser dependency).
 * Parses {@code openapi.yaml} at the repo root.
 */
final class OpenApiContractSupport {

    private static final Path OPENAPI = Path.of("..", "openapi.yaml");

    private OpenApiContractSupport() {
    }

    static String loadOpenApiYaml() {
        try {
            return Files.readString(OPENAPI);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read " + OPENAPI.toAbsolutePath(), ex);
        }
    }

    static Set<String> enumValues(final String yaml, final String enumName) {
        final Pattern block = Pattern.compile(
                "^    " + Pattern.quote(enumName) + ":\\s*\\R"
                        + "      type: string\\s*\\R"
                        + "      enum:\\s*\\R"
                        + "((?:        - .+\\R)+)",
                Pattern.MULTILINE);
        final Matcher matcher = block.matcher(yaml);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Enum not found in openapi.yaml: " + enumName);
        }
        final Set<String> values = new LinkedHashSet<>();
        for (final String line : matcher.group(1).split("\\R")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                values.add(trimmed.substring(2).trim());
            }
        }
        return values;
    }

    static Set<String> schemaProperties(final String yaml, final String schemaName) {
        final Pattern start = Pattern.compile(
                "^    " + Pattern.quote(schemaName) + ":\\s*\\R"
                        + "      type: object\\s*\\R"
                        + "(?:      (?!properties:).+\\R)*"
                        + "      properties:\\s*\\R",
                Pattern.MULTILINE);
        final Matcher startMatcher = start.matcher(yaml);
        if (!startMatcher.find()) {
            throw new IllegalArgumentException("Schema not found in openapi.yaml: " + schemaName);
        }
        final int from = startMatcher.end();
        final String tail = yaml.substring(from);
        final Set<String> props = new LinkedHashSet<>();
        final Pattern propLine = Pattern.compile("^        (\\w+):");
        for (final String line : tail.split("\\R", -1)) {
            if (line.isBlank()) {
                break;
            }
            if (!line.startsWith("        ")) {
                break;
            }
            final Matcher prop = propLine.matcher(line);
            if (prop.find()) {
                props.add(prop.group(1));
            }
        }
        return props;
    }

    static Set<String> schemaRequired(final String yaml, final String schemaName) {
        final Pattern block = Pattern.compile(
                "^    " + Pattern.quote(schemaName) + ":[\\s\\S]*?"
                        + "^      required: \\[(.+)\\]",
                Pattern.MULTILINE);
        final Matcher matcher = block.matcher(yaml);
        if (!matcher.find()) {
            return Set.of();
        }
        final Set<String> required = new LinkedHashSet<>();
        for (final String part : matcher.group(1).split(",")) {
            required.add(part.trim());
        }
        return required;
    }

    static Set<String> vendorJsonMediaTypes(final String yaml) {
        final Pattern mime = Pattern.compile("(application/vnd\\.paw\\.[^\\s\"']+\\+json)");
        final Matcher matcher = mime.matcher(yaml);
        final Set<String> types = new LinkedHashSet<>();
        while (matcher.find()) {
            final String type = matcher.group(1);
            if (!type.contains("<entidad>")) {
                types.add(type);
            }
        }
        return types;
    }

    static Set<String> jsonFieldNames(final String json, final com.fasterxml.jackson.databind.ObjectMapper mapper)
            throws IOException {
        final com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
        return root.fieldNames().hasNext()
                ? java.util.stream.StreamSupport.stream(
                                java.util.Spliterators.spliteratorUnknownSize(root.fieldNames(), 0), false)
                        .collect(Collectors.toCollection(LinkedHashSet::new))
                : Set.of();
    }
}
