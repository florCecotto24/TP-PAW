package ar.edu.itba.paw.webapp.support;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import ar.edu.itba.paw.models.util.time.WallDateTimeParsing;

/** Normalizes openapi ISO date-times into wall-local strings understood by reservation services. */
public final class ReservationRestDateTimes {

    private ReservationRestDateTimes() {
    }

    public static String toWallLocalInput(final String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        final String trimmed = raw.trim();
        try {
            return WallDateTimeParsing.formatUtcAsClientWallDateTimeInput(OffsetDateTime.parse(trimmed));
        } catch (final DateTimeParseException ignored) {
            // fall through
        }
        try {
            return WallDateTimeParsing.formatUtcAsClientWallDateTimeInput(
                    Instant.parse(trimmed).atOffset(ZoneOffset.UTC));
        } catch (final DateTimeParseException ignored) {
            return trimmed;
        }
    }
}
