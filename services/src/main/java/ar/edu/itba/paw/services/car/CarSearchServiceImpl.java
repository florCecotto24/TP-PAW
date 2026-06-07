package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.WallDateTimeParsing;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.NeighborhoodNameMatcher;

import ar.edu.itba.paw.services.location.LocationService;
/**
 * Search-criteria construction service. Architectural rule: this impl no longer touches
 * {@code CarDao} — the actual query/browse methods (cheapest, most-recent, search, owner cards,
 * similar) live on {@link CarServiceImpl} which owns the DAO.
 *
 * <p>Pagination rule: this impl does not read any pagination policy. Controllers inject
 * {@code AppPaginationProperties} (webapp) and pass {@code uiPageSize}/{@code pageSize} into the
 * builder methods. The persistence layer reads its own DB fetch window.</p>
 */
@Service
public final class CarSearchServiceImpl implements CarSearchService {

    private static final Set<String> RATING_BANDS = Set.of("UNDER_2", "2_TO_3", "3_TO_4", "OVER_4");

    private final ReservationTimingPolicy reservationTimingPolicy;
    private final LocationService locationService;

    @Autowired
    public CarSearchServiceImpl(
            final ReservationTimingPolicy reservationTimingPolicy,
            final LocationService locationService) {
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.locationService = locationService;
    }

    @Override
    @Transactional(readOnly = true)
    public CarSearchCriteria buildSearchCriteria(final CarSearchRequest request) {
        final String query = request.getQuery();
        final String from = request.getFrom();
        final String until = request.getUntil();
        final boolean flexible = request.isFlexible();
        final String flexMonth = request.getFlexMonth();
        final Integer flexDays = request.getFlexDays();
        final BigDecimal priceMin = request.getPriceMin();
        final BigDecimal priceMax = request.getPriceMax();
        final String sort = request.getSort();

        final List<String> transmissions = enumNames(request.getTransmissions());
        final List<String> powertrains = enumNames(request.getPowertrains());
        final List<String> mergedCarTypes = enumNames(request.getCategories());
        final BigDecimal minPrice = priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0 ? priceMin : null;
        final BigDecimal maxPrice = priceMax != null && priceMax.compareTo(BigDecimal.ZERO) >= 0 ? priceMax : null;
        final List<String> ratingBands = collectRatingBandParams(request.getRatingBands());
        final String[] sortParts = (sort != null && !sort.isBlank()) ? sort.split(",", 2) : new String[0];
        final String sortBy = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        final LocalDate browseWallDate = publicBrowseMinBookableWallDate();
        final List<Long> mergedNeighborhoodIds = mergeNeighborhoodIdsForSearch(query, request.getNeighborhoodIds());
        final CarSearchCriteria.Builder builder = CarSearchCriteria.builder()
                .query(query)
                .transmissions(transmissions)
                .powertrains(powertrains)
                .carTypes(mergedCarTypes)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .ratingBands(ratingBands)
                .page(request.getPage())
                .uiPageSize(request.getUiPageSize())
                .sortBy(sortBy)
                .sortDirection(sortDir)
                .browseWallDate(browseWallDate)
                .excludeOwnerUserId(null)
                .neighborhoodIds(mergedNeighborhoodIds);
        if (flexible && flexMonth != null && !flexMonth.isBlank()) {
            java.time.YearMonth parsedMonth = null;
            try {
                parsedMonth = YearMonth.parse(flexMonth);
            } catch (final java.time.format.DateTimeParseException ignored) {
                // invalid month string — fall back to no filter
            }
            if (parsedMonth != null) {
                final Integer clampedDays = flexDays != null && flexDays >= 1
                        && flexDays <= parsedMonth.lengthOfMonth() ? flexDays : null;
                builder.flexibleMonth(parsedMonth).flexibleDays(clampedDays);
            }
        } else {
            Instant rangeStart = WallDateTimeParsing.parseSearchFilterRangeStartInstant(from);
            Instant rangeEndExclusive = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(until);
            if (rangeStart != null && rangeEndExclusive != null && !rangeEndExclusive.isAfter(rangeStart)) {
                final Instant rs = WallDateTimeParsing.parseSearchFilterRangeStartInstant(until);
                final Instant re = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(from);
                rangeStart = rs;
                rangeEndExclusive = re;
            }
            if (rangeStart == null || rangeEndExclusive == null || !rangeEndExclusive.isAfter(rangeStart)) {
                rangeStart = null;
                rangeEndExclusive = null;
            }
            builder.availabilityRange(rangeStart, rangeEndExclusive);
        }
        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<Car.Status> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final int pageSize,
            final String sort) {
        // /my-cars hub: surfacing cars with pending refund-proof obligations at the top is the desired UX.
        // The counterparty profile grid uses the overload that takes excludeCarId and keeps the flag false.
        return buildOwnerCarSearchCriteriaInternal(
                ownerId, category, transmission, powertrain, priceMin, priceMax,
                carStatus, rating, textQuery, page, pageSize, sort, null, /* prioritizeRefundPending */ true);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<Car.Status> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final int pageSize,
            final String sort,
            final Long excludeCarId) {
        return buildOwnerCarSearchCriteriaInternal(
                ownerId, category, transmission, powertrain, priceMin, priceMax,
                carStatus, rating, textQuery, page, pageSize, sort, excludeCarId,
                /* prioritizeRefundPending */ false);
    }

    @Override
    public LocalDate publicBrowseMinBookableWallDate() {
        return LocalDate.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS),
                AppTimezone.WALL_ZONE);
    }

    private OwnerCarSearchCriteria buildOwnerCarSearchCriteriaInternal(
            final long ownerId,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<Car.Status> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final int pageSize,
            final String sort,
            final Long excludeCarId,
            final boolean prioritizeRefundPending) {
        final List<String> carTypes = enumNames(category);
        final List<String> transmissions = enumNames(transmission);
        final List<String> powertrains = enumNames(powertrain);
        final BigDecimal minPrice = priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0 ? priceMin : null;
        final BigDecimal maxPrice = priceMax != null && priceMax.compareTo(BigDecimal.ZERO) >= 0 ? priceMax : null;
        // OwnerCarSearchCriteria persists statuses as the lowercase DB token (matches Car.Status
        // converter); the previous LISTING_STATUSES whitelist hard-coded {active,paused,finished}
        // which mismatched the dropdown options exposed by CarEnumOptions. Trusting the typed enum
        // list lets every Car.Status value flow through correctly.
        final List<String> statuses = enumDbTokens(carStatus);
        final List<String> ratingBands = collectRatingBandParams(rating);
        final String[] sortParts = (sort != null && !sort.isBlank()) ? sort.split(",", 2) : new String[0];
        final String sortBy = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        return OwnerCarSearchCriteria.builderFor(ownerId)
                .page(page)
                .pageSize(pageSize)
                .carStatusFilters(statuses)
                .textQuery(textQuery)
                .carTypes(carTypes)
                .transmissions(transmissions)
                .powertrains(powertrains)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .ratingBands(ratingBands)
                .sortBy(sortBy)
                .sortDirection(sortDir)
                .excludeCarId(excludeCarId)
                .prioritizeRefundPending(prioritizeRefundPending)
                .build();
    }

    private List<Long> mergeNeighborhoodIdsForSearch(
            final String query, final List<Long> explicitNeighborhoodIds) {
        final LinkedHashSet<Long> merged = new LinkedHashSet<>();
        if (explicitNeighborhoodIds != null) {
            for (final Long id : explicitNeighborhoodIds) {
                if (id != null && id > 0L && locationService.findNeighborhoodById(id).isPresent()) {
                    merged.add(id);
                }
            }
        }
        final String q = query != null ? query.trim() : "";
        if (!q.isEmpty()) {
            merged.addAll(NeighborhoodNameMatcher.idsMatchingFuzzyTokens(
                    q,
                    locationService.findAllNeighborhoods(),
                    2,
                    3));
        }
        return List.copyOf(merged);
    }

    /**
     * Distinct {@code Enum.name()} tokens for any non-null entry; preserves order, drops nulls.
     * Matches the wire format the criteria DTO and JPQL/native filters in {@code CarJpaDao} expect
     * (e.g. {@code SEDAN}, {@code AUTOMATIC}, {@code GASOLINE}).
     */
    private static <E extends Enum<E>> List<String> enumNames(final List<E> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        final LinkedHashSet<String> out = new LinkedHashSet<>();
        for (final E e : raw) {
            if (e != null) {
                out.add(e.name());
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * Lowercase DB tokens for any non-null entry (matches {@code Car.StatusConverter} /
     * {@code Reservation.StatusConverter} which persist {@code name().toLowerCase()}). Used by
     * {@code OwnerCarSearchCriteria.carStatusFilters} which feeds a native SQL {@code IN} clause
     * over {@code cars.status}.
     */
    private static <E extends Enum<E>> List<String> enumDbTokens(final List<E> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        final LinkedHashSet<String> out = new LinkedHashSet<>();
        for (final E e : raw) {
            if (e != null) {
                out.add(e.name().toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(out);
    }

    private static List<String> collectRatingBandParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            if (RATING_BANDS.contains(u)) {
                out.add(u);
            }
        }
        return out;
    }
}
