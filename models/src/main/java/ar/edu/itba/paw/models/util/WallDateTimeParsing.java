package ar.edu.itba.paw.models.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

public final class WallDateTimeParsing {

    private WallDateTimeParsing() {
    }

    /** Pattern for HTML {@code datetime-local} and search inputs (no seconds). */
    public static final String WALL_INPUT_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm";

    public static final DateTimeFormatter WALL_INPUT_DATE_TIME = DateTimeFormatter.ofPattern(WALL_INPUT_DATE_TIME_PATTERN);

    /** Date-only filter input: ISO local date. */
    public static final DateTimeFormatter WALL_INPUT_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Human-readable range labels in the car detail UI. */
    public static final DateTimeFormatter WALL_DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Day-only labels (listing availability windows). */
    public static final DateTimeFormatter WALL_DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Parses a wall-local date-time from forms (ISO-8601 local, typically {@code yyyy-MM-ddTHH:mm}) to UTC.
     *
     * @throws DateTimeParseException if the value is not a valid local date-time
     */
    public static OffsetDateTime parseWallLocalDateTimeToUtc(final String value) {
        final LocalDateTime localDateTime = LocalDateTime.parse(value.trim());
        return localDateTime.atZone(AvailabilityPeriod.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
    }

    /**
     * Search filter: start of the inclusive range as an instant in UTC (wall zone).
     * Accepts ISO date or {@link #WALL_INPUT_DATE_TIME}.
     *
     * @return {@code null} if blank or unparseable
     */
    public static Instant parseSearchFilterRangeStartInstant(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim();
        try {
            if (!t.contains("T")) {
                return LocalDate.parse(t, WALL_INPUT_DATE).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant();
            }
            return LocalDateTime.parse(t, WALL_INPUT_DATE_TIME).atZone(AvailabilityPeriod.WALL_ZONE).toInstant();
        } catch (final DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Search filter: exclusive end of the range as an instant in UTC (wall zone).
     * Date-only inputs use start of next day; date-time inputs use start of the minute after {@code t}.
     *
     * @return {@code null} if blank or unparseable
     */
    public static Instant parseSearchFilterRangeEndExclusiveInstant(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String t = raw.trim();
        try {
            if (!t.contains("T")) {
                return LocalDate.parse(t, WALL_INPUT_DATE).plusDays(1).atStartOfDay(AvailabilityPeriod.WALL_ZONE).toInstant();
            }
            return LocalDateTime.parse(t, WALL_INPUT_DATE_TIME)
                    .atZone(AvailabilityPeriod.WALL_ZONE)
                    .plusMinutes(1)
                    .toInstant();
        } catch (final DateTimeParseException e) {
            return null;
        }
    }
}
