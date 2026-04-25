package ar.edu.itba.paw.webapp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BookableWallRangesJson {

    private BookableWallRangesJson() {
    }

    public static List<LocalDateSegment> mergeAdjacentSegments(final List<LocalDateSegment> raw) {
        if (raw.isEmpty()) {
            return List.of();
        }
        final List<LocalDateSegment> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparing(LocalDateSegment::startInclusive));
        final List<LocalDateSegment> out = new ArrayList<>();
        LocalDateSegment cur = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            final LocalDateSegment next = sorted.get(i);
            if (!next.startInclusive.isAfter(cur.endInclusive.plusDays(1))) {
                final LocalDate newEnd = cur.endInclusive.isBefore(next.endInclusive) ? next.endInclusive : cur.endInclusive;
                cur = new LocalDateSegment(cur.startInclusive, newEnd);
            } else {
                out.add(cur);
                cur = next;
            }
        }
        out.add(cur);
        return out;
    }

    /**
     * Recorta el inicio de cada segmento para que el primer día seleccionable como retiro cumpla la misma regla
     * que {@code ReservationServiceImpl}: instante de retiro &gt; {@code minPickupExclusive}.
     *
     * @param pickupWallTime hora de retiro publicada (muro); si es null se usa 10:00 como en el formulario
     */
    public static List<LocalDateSegment> clipSegmentsToMinPickupInstant(
            final List<LocalDateSegment> merged,
            final LocalTime pickupWallTime,
            final ZoneId wallZone,
            final Instant minPickupExclusive) {
        if (merged.isEmpty()) {
            return List.of();
        }
        final LocalTime pickup = pickupWallTime != null ? pickupWallTime : LocalTime.of(10, 0);
        final List<LocalDateSegment> clipped = new ArrayList<>();
        for (final LocalDateSegment seg : merged) {
            LocalDate d = seg.startInclusive();
            while (!d.isAfter(seg.endInclusive())) {
                final Instant pickupInstant = ZonedDateTime.of(d, pickup, wallZone).toInstant();
                if (pickupInstant.isAfter(minPickupExclusive)) {
                    clipped.add(new LocalDateSegment(d, seg.endInclusive()));
                    break;
                }
                d = d.plusDays(1);
            }
        }
        return mergeAdjacentSegments(clipped);
    }

    public static String toJsonArray(final List<LocalDateSegment> segments) {
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            final LocalDateSegment s = segments.get(i);
            sb.append("{\"from\":\"").append(s.startInclusive()).append("\",\"to\":\"").append(s.endInclusive()).append("\"}");
        }
        sb.append(']');
        return sb.toString();
    }

    public record LocalDateSegment(LocalDate startInclusive, LocalDate endInclusive) {
    }
}
