package ar.edu.itba.paw.models.util.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * Format for dates/times for UI and emails: Argentina wall-clock, without seconds, pattern according to language.
 */
public final class WallDateTimeDisplayFormat {

    private static final Logger LOG = LoggerFactory.getLogger(WallDateTimeDisplayFormat.class);

    private static final DateTimeFormatter ES_WALL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter EN_WALL = DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.ENGLISH);

    private WallDateTimeDisplayFormat() {
    }

    public static String formatUtcAsWallLocalNoSeconds(final OffsetDateTime utc, final Locale locale) {
        if (utc == null) {
            return "";
        }
        final LocalDateTime wall = utc.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDateTime();
        return formatWallLocalNoSeconds(wall, locale);
    }

    public static String formatWallLocalNoSeconds(final LocalDateTime wallLocal, final Locale locale) {
        if (wallLocal == null) {
            return "";
        }
        return formatterFor(locale).format(wallLocal);
    }

    /**
     * Interprets the typical value of forms ({@link WallDateTimeParsing#WALL_INPUT_DATE_TIME}) and displays it without seconds.
     *
     * @return empty string if {@code raw} is null or blank; if it cannot be parsed, returns {@code raw} truncated
     */
    public static String formatClientWallDateTimeInputOrRaw(final String raw, final Locale locale) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        final String t = raw.trim();
        try {
            final LocalDateTime ldt = LocalDateTime.parse(t, WallDateTimeParsing.WALL_INPUT_DATE_TIME);
            return formatWallLocalNoSeconds(ldt, locale);
        } catch (final DateTimeParseException e) {
            LOG.atDebug()
                    .setMessage("Could not format client wall datetime input as {} ; returning raw [{}]")
                    .addArgument(WallDateTimeParsing.WALL_INPUT_DATE_TIME_PATTERN)
                    .addArgument(t)
                    .setCause(e)
                    .log();
            return t;
        }
    }

    private static DateTimeFormatter formatterFor(final Locale locale) {
        final Locale l = locale != null ? locale : Locale.ENGLISH;
        if ("es".equalsIgnoreCase(l.getLanguage())) {
            return ES_WALL;
        }
        return EN_WALL.withLocale(Locale.ENGLISH);
    }
}
