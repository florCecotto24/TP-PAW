package ar.edu.itba.paw.webapp.util;

import java.util.List;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * JSON for Flatpick / car-detail date bounds. Availability clipping and merging live in the service layer
 * ({@link ar.edu.itba.paw.services.ListingService#getBookableWallAvailabilityPeriodsForRiderDatePicker(long, java.time.LocalTime, java.time.Instant)}).
 */
public final class BookableWallRangesJson {

    private BookableWallRangesJson() {
    }

    public static String toJsonArray(final List<AvailabilityPeriod> segments) {
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            final AvailabilityPeriod s = segments.get(i);
            sb.append("{\"from\":\"")
                    .append(s.getStartInclusive())
                    .append("\",\"to\":\"")
                    .append(s.getEndInclusive())
                    .append("\"}");
        }
        sb.append(']');
        return sb.toString();
    }
}
