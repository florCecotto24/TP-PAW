package ar.edu.itba.paw.models.domain.car;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.WallDateTimeParsing;

/**
 * Normalized wall-zone date range for availability UIs and JSON; the business calendar zone for
 * listings and reservations lives in {@link AppTimezone#WALL_ZONE}.
 */
public final class AvailabilityPeriod {

    private final LocalDate startInclusive;
    private final LocalDate endInclusive;

    public AvailabilityPeriod(final LocalDate startInclusive, final LocalDate endInclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive);
        this.endInclusive = Objects.requireNonNull(endInclusive);
    }

    /** Static factory preferred over ad-hoc construction at the HTTP boundary. */
    public static AvailabilityPeriod of(final LocalDate startInclusive, final LocalDate endInclusive) {
        return new AvailabilityPeriod(startInclusive, endInclusive);
    }

    public LocalDate getStartInclusive() {
        return startInclusive;
    }

    public LocalDate getEndInclusive() {
        return endInclusive;
    }

    public boolean isValidOrder() {
        return !endInclusive.isBefore(startInclusive);
    }

    public static OffsetDateTime parseWallLocalDateTimeToUtc(final String value) {
        return WallDateTimeParsing.parseWallLocalDateTimeToUtc(value);
    }
}
