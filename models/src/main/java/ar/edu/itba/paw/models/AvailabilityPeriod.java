package ar.edu.itba.paw.models;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class AvailabilityPeriod {

    public static final ZoneId WALL_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final LocalDateTime startInclusive;
    private final LocalDateTime endInclusive;

    public AvailabilityPeriod(final LocalDateTime startInclusive, final LocalDateTime endInclusive) {
        this.startInclusive = Objects.requireNonNull(startInclusive);
        this.endInclusive = Objects.requireNonNull(endInclusive);
    }

    public LocalDateTime getStartInclusive() {
        return startInclusive;
    }

    public LocalDateTime getEndInclusive() {
        return endInclusive;
    }

    public boolean isValidOrder() {
        return !endInclusive.isBefore(startInclusive);
    }

    public OffsetDateTime startInstantUtc() {
        return startInclusive.atZone(WALL_ZONE).toInstant().atOffset(ZoneOffset.UTC);
    }

    public OffsetDateTime endExclusiveInstantUtc() {
        return endInclusive.atZone(WALL_ZONE).plusMinutes(1).toInstant().atOffset(ZoneOffset.UTC);
    }
}
