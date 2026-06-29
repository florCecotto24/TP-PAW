package ar.edu.itba.paw.services.car;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;

/**
 * Public browse/search without explicit rider dates: hide cars that have no bookable wall day on or
 * after the pickup-lead-adjusted floor ({@link CarSearchService#publicBrowseMinBookableWallDate()}).
 */
final class BookableBrowseSupport {

    private BookableBrowseSupport() {
    }

    static Page<CarCard> paginateBookableCards(
            final BiFunction<Integer, Integer, Page<CarCard>> fetchPage,
            final int page,
            final int pageSize,
            final LocalDate minBookableWallDate,
            final CarAvailabilityService carAvailabilityService) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        final List<CarCard> allBookable = collectAllBookableCards(
                fetchPage, minBookableWallDate, safePageSize, carAvailabilityService);
        final int from = safePage * safePageSize;
        final int to = Math.min(from + safePageSize, allBookable.size());
        final List<CarCard> slice = from >= allBookable.size() ? List.of() : allBookable.subList(from, to);
        return new Page<>(slice, safePage, safePageSize, allBookable.size());
    }

    static List<CarCard> retainBookableCards(
            final List<CarCard> cards,
            final LocalDate minBookableWallDate,
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

    static boolean needsBookableDayFilter(final ar.edu.itba.paw.models.util.search.CarSearchCriteria criteria) {
        return criteria.getBrowseWallDate() != null && !criteria.hasAvailabilityRange();
    }

    private static List<CarCard> collectAllBookableCards(
            final BiFunction<Integer, Integer, Page<CarCard>> fetchPage,
            final LocalDate minBookableWallDate,
            final int pageSize,
            final CarAvailabilityService carAvailabilityService) {
        final List<CarCard> allBookable = new ArrayList<>();
        int sourcePage = 0;
        final int batchSize = Math.max(pageSize * 3, 24);
        while (true) {
            final Page<CarCard> batch = fetchPage.apply(sourcePage++, batchSize);
            if (batch.getContent().isEmpty()) {
                break;
            }
            allBookable.addAll(retainBookableCards(batch.getContent(), minBookableWallDate, carAvailabilityService));
            if (batch.getContent().size() < batchSize) {
                break;
            }
        }
        return allBookable;
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
