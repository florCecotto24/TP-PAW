package ar.edu.itba.paw.models;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

public final class AvailabilityPeriod {

    public static final ZoneId WALL_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final LocalDate startInclusive;
    private final LocalDate endInclusive;

    public AvailabilityPeriod(final LocalDate startInclusive, final LocalDate endInclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive);
        this.endInclusive = Objects.requireNonNull(endInclusive);
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
