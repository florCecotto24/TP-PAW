package ar.edu.itba.paw.webapp.util;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses nested catalog model URNs ({@code /brands/{brandId}/models/{modelId}}). */
public final class ModelUriSupport {

    private static final Pattern NESTED_MODEL_PATH =
            Pattern.compile(".*/brands/(\\d+)/models/(\\d+)/?$");

    private ModelUriSupport() {
    }

    /** Model id from a canonical nested {@code modelUri}. */
    public static long parseModelId(final String modelUri) {
        final URI uri = URI.create(modelUri.trim());
        final String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("modelUri path is empty");
        }
        final Matcher matcher = NESTED_MODEL_PATH.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("modelUri must be /brands/{brandId}/models/{modelId}");
        }
        final long modelId = Long.parseLong(matcher.group(2));
        if (modelId <= 0) {
            throw new IllegalArgumentException("modelId must be positive");
        }
        return modelId;
    }
}
