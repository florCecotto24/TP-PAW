package ar.edu.itba.paw.services.car;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.car.CarCard;

/**
 * Public browse/search without explicit rider dates relies on the DAO SQL predicate
 * {@code offered availability end_date >= browseWallDate} (COUNT and ID page share it).
 * This helper only applies a bounded in-memory bookable check for small result sets
 * (e.g. similar cars), never materialising the full catalogue.
 */
final class BookableBrowseSupport {

    private BookableBrowseSupport() {
    }

    /**
     * Keeps cards that still have at least one rider-bookable segment. Callers must pass a
     * bounded list (pageSize / similar limit) — never the full catalogue.
     */
    static List<CarCard> retainBookableCards(
            final List<CarCard> cards,
            final CarAvailabilityService carAvailabilityService) {
        if (cards.isEmpty()) {
            return cards;
        }
        final Map<Long, Boolean> riderBookableByCarId =
                carAvailabilityService.hasRiderBookableSegmentsByCarIds(
                        cards.stream().map(CarCard::getCarId).toList(), Instant.now());
        return cards.stream()
                .filter(card -> Boolean.TRUE.equals(riderBookableByCarId.get(card.getCarId())))
                .toList();
    }

    static boolean hasBookableWallDayOnOrAfter(
            final List<AvailabilityPeriod> periods, final LocalDate minWallDate) {
        if (periods == null || periods.isEmpty() || minWallDate == null) {
            return false;
        }
        for (final AvailabilityPeriod period : periods) {
            if (period.getEndInclusive().isBefore(minWallDate)) {
                continue;
            }
            final LocalDate effectiveStart = period.getStartInclusive().isBefore(minWallDate)
                    ? minWallDate
                    : period.getStartInclusive();
            if (!effectiveStart.isAfter(period.getEndInclusive())) {
                return true;
            }
        }
        return false;
    }
}
