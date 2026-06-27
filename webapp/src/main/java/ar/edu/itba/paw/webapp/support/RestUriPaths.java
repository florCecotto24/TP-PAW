package ar.edu.itba.paw.webapp.support;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Parses canonical URNs from hypermedia links ({@code /cars/5}, {@code /cars/5/availabilities/9}, …).
 */
public final class RestUriPaths {

    public record CarAvailabilityIds(long carId, long availabilityId) {
    }

    private RestUriPaths() {
    }

    public static long parseCarId(final String carUri) {
        return parseIdAfterSegment(normalizePath(carUri), "cars");
    }

    public static CarAvailabilityIds parseAvailabilityUri(final String availabilityUri) {
        final List<String> segments = segments(normalizePath(availabilityUri));
        final int carsIdx = indexOf(segments, "cars");
        final int availIdx = indexOfAfter(segments, "availabilities", carsIdx);
        if (carsIdx < 0 || availIdx < 0 || carsIdx + 1 >= segments.size() || availIdx + 1 >= segments.size()) {
            throw new javax.ws.rs.BadRequestException("Invalid availabilityUri.");
        }
        final long carId = parsePositiveLong(segments.get(carsIdx + 1), "carUri");
        final long availabilityId = parsePositiveLong(segments.get(availIdx + 1), "availabilityUri");
        return new CarAvailabilityIds(carId, availabilityId);
    }

    private static long parseIdAfterSegment(final String path, final String segment) {
        final List<String> segments = segments(path);
        final int idx = indexOf(segments, segment);
        if (idx < 0 || idx + 1 >= segments.size()) {
            throw new javax.ws.rs.BadRequestException("Invalid URI: missing " + segment + " id.");
        }
        return parsePositiveLong(segments.get(idx + 1), segment);
    }

    private static String normalizePath(final String uri) {
        if (uri == null || uri.isBlank()) {
            throw new javax.ws.rs.BadRequestException("URI is required.");
        }
        String path = uri.trim();
        if (path.contains("://")) {
            path = URI.create(path).getPath();
        }
        final int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        return path;
    }

    private static List<String> segments(final String path) {
        return Arrays.stream(path.split("/"))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static int indexOf(final List<String> segments, final String name) {
        return segments.indexOf(name);
    }

    private static int indexOfAfter(final List<String> segments, final String name, final int after) {
        for (int i = Math.max(0, after + 1); i < segments.size(); i++) {
            if (name.equals(segments.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static long parsePositiveLong(final String raw, final String label) {
        try {
            final long value = Long.parseLong(raw);
            if (value <= 0) {
                throw new NumberFormatException("non-positive");
            }
            return value;
        } catch (final NumberFormatException ex) {
            throw new javax.ws.rs.BadRequestException("Invalid " + label + " id.");
        }
    }
}
