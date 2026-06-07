package ar.edu.itba.paw.models.util.time;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;

/**
 * JSON for Flatpickr / car-detail date bounds. Each segment carries the effective per-day attributes
 * (price, pickup/return wall times, public pickup location) so the client can derive subtotal/total and
 * the actual check-in/check-out values for any rider-selected range without consulting the server again.
 *
 * <p>Segment generation, day-effective resolution (most recently created OFFERED availability that covers
 * the day), and pickup-lead clipping live in the service layer
 * ({@code CarAvailabilityService#getBookableSegmentsForRiderDatePickerByCar}).</p>
 */
public final class BookableWallRangesJson {

    private BookableWallRangesJson() {
    }

    /**
     * Date-only JSON array of {@code [{from, to}, ...]} ranges, suitable for Flatpickr's
     * {@code disable} option. Used to feed the owner publish picker with the wall-day ranges
     * that already hold active reservations.
     */
    public static String availabilityPeriodsToJsonArray(final List<AvailabilityPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < periods.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            final AvailabilityPeriod p = periods.get(i);
            sb.append("{\"from\":\"")
                    .append(p.getStartInclusive())
                    .append("\",\"to\":\"")
                    .append(p.getEndInclusive())
                    .append("\"}");
        }
        sb.append(']');
        return sb.toString();
    }

    public static String toJsonArray(final List<BookableSegmentProjection> segments) {
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            final BookableSegmentProjection s = segments.get(i);
            sb.append("{\"from\":\"")
                    .append(s.getFrom())
                    .append("\",\"to\":\"")
                    .append(s.getTo())
                    .append("\",\"dayPrice\":")
                    .append(dayPriceToJson(s.getDayPrice()))
                    .append(",\"checkInTime\":")
                    .append(timeToJson(s.getCheckInTime()))
                    .append(",\"checkOutTime\":")
                    .append(timeToJson(s.getCheckOutTime()))
                    .append(",\"location\":\"")
                    .append(escapeJsonString(s.getPublicLocation()))
                    .append("\",\"neighborhoodId\":")
                    .append(s.getNeighborhoodId() != null ? s.getNeighborhoodId() : "null")
                    .append("}");
        }
        sb.append(']');
        return sb.toString();
    }

    private static String dayPriceToJson(final BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }

    private static String timeToJson(final java.time.LocalTime t) {
        if (t == null) {
            return "null";
        }
        return "\"" + String.format("%02d:%02d", t.getHour(), t.getMinute()) + "\"";
    }

    private static String escapeJsonString(final String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(raw.length() + 4);
        for (final char c : raw.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
