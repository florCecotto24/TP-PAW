package ar.edu.itba.paw.models.util.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;

/**
 * Parses and formats wall-zone date/time strings from HTML inputs and maps instants to UTC using
 * {@link AvailabilityPeriod#WALL_ZONE}.
 */
public final class WallDateTimeParsing {

    private static final Logger LOG = LoggerFactory.getLogger(WallDateTimeParsing.class);

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
        return localDateTime.atZone(AppTimezone.WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
    }

    /**
     * Inverse of {@link #parseWallLocalDateTimeToUtc(String)}: renders a UTC instant back into
     * the {@link #WALL_INPUT_DATE_TIME_PATTERN} string in the application wall zone, so that
     * persisted reservation timestamps can re-populate confirmation pages reached via a
     * post-redirect-get without needing the original form values.
     *
     * @return empty string when {@code utc} is null
     */
    public static String formatUtcAsClientWallDateTimeInput(final OffsetDateTime utc) {
        if (utc == null) {
            return "";
        }
        return WALL_INPUT_DATE_TIME.format(utc.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDateTime());
    }

    /**
     * Wall-zone calendar day for a date-time coming from a form input (same format as
     * {@link #parseWallLocalDateTimeToUtc}). Mirrors the null-safe convention of the
     * {@code parseSearchFilter*} helpers above so callers can use it directly on raw form values.
     *
     * @return {@code null} when the input is blank or unparseable
     */
    public static LocalDate parseWallLocalDateTimeToWallDate(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return parseWallLocalDateTimeToUtc(value)
                    .atZoneSameInstant(AppTimezone.WALL_ZONE)
                    .toLocalDate();
        } catch (final DateTimeParseException e) {
            LOG.atDebug()
                    .setMessage("Unparseable wall date-time [{}]")
                    .addArgument(value)
                    .setCause(e)
                    .log();
            return null;
        }
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
                return LocalDate.parse(t, WALL_INPUT_DATE).atStartOfDay(AppTimezone.WALL_ZONE).toInstant();
            }
            return LocalDateTime.parse(t, WALL_INPUT_DATE_TIME).atZone(AppTimezone.WALL_ZONE).toInstant();
        } catch (final DateTimeParseException e) {
            LOG.atDebug()
                    .setMessage("Unparseable search filter range start [{}]")
                    .addArgument(t)
                    .setCause(e)
                    .log();
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
                return LocalDate.parse(t, WALL_INPUT_DATE).plusDays(1).atStartOfDay(AppTimezone.WALL_ZONE).toInstant();
            }
            return LocalDateTime.parse(t, WALL_INPUT_DATE_TIME)
                    .atZone(AppTimezone.WALL_ZONE)
                    .plusMinutes(1)
                    .toInstant();
        } catch (final DateTimeParseException e) {
            LOG.atDebug()
                    .setMessage("Unparseable search filter range end [{}]")
                    .addArgument(t)
                    .setCause(e)
                    .log();
            return null;
        }
    }
}
