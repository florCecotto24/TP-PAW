package ar.edu.itba.paw.webapp.util;

import java.time.LocalDate;
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
