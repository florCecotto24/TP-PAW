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

    /**
     * Status codes declared under {@code responses:} for a path operation (e.g. {@code post} on {@code /credentials}).
     */
    static Set<String> operationResponseStatusCodes(
            final String yaml, final String path, final String httpMethod) {
        final Pattern pathBlock = Pattern.compile(
                "^  " + Pattern.quote(path) + ":\\s*\\R([\\s\\S]*?)(?=^  /|^  #|^components:|\\Z)",
                Pattern.MULTILINE);
        final Matcher pathMatcher = pathBlock.matcher(yaml);
        if (!pathMatcher.find()) {
            throw new IllegalArgumentException("Path not found in openapi.yaml: " + path);
        }
        final String block = pathMatcher.group(1);
        final Pattern operation = Pattern.compile(
                "^    " + Pattern.quote(httpMethod) + ":\\s*\\R([\\s\\S]*?)(?=^    [a-z]+:|^  /|^  #|^components:|\\Z)",
                Pattern.MULTILINE);
        final Matcher operationMatcher = operation.matcher(block);
        if (!operationMatcher.find()) {
            throw new IllegalArgumentException("Operation not found in openapi.yaml: " + httpMethod + " " + path);
        }
        final String operationBlock = operationMatcher.group(1);
        final int responsesIdx = operationBlock.indexOf("responses:");
        if (responsesIdx < 0) {
            throw new IllegalArgumentException("responses not found for " + httpMethod + " " + path);
        }
        final String responsesTail = operationBlock.substring(responsesIdx);
        final Pattern statusLine = Pattern.compile("^        \"(\\d{3})\":", Pattern.MULTILINE);
        final Matcher statusMatcher = statusLine.matcher(responsesTail);
        final Set<String> codes = new LinkedHashSet<>();
        while (statusMatcher.find()) {
            codes.add(statusMatcher.group(1));
        }
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("No response status codes for " + httpMethod + " " + path);
        }
        return codes;
    }

    static int parameterSchemaInt(final String yaml, final String parameterComponentName, final String field) {
        final Pattern block = Pattern.compile(
                "^    " + Pattern.quote(parameterComponentName) + ":[\\s\\S]*?(?=^    [a-zA-Z]+:|^  [a-z#]|\\Z)",
                Pattern.MULTILINE);
        final Matcher blockMatcher = block.matcher(yaml);
        if (!blockMatcher.find()) {
            throw new IllegalArgumentException("Parameter not found in openapi.yaml: " + parameterComponentName);
        }
        final Pattern fieldPattern = Pattern.compile("\\b" + Pattern.quote(field) + ": (\\d+)");
        final Matcher fieldMatcher = fieldPattern.matcher(blockMatcher.group());
        if (!fieldMatcher.find()) {
            throw new IllegalArgumentException(
                    "Parameter schema field not found in openapi.yaml: " + parameterComponentName + "." + field);
        }
        return Integer.parseInt(fieldMatcher.group(1));
    }
}
