package ar.edu.itba.paw.services.car;


import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.dto.reservation.BlockingReservationProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.BookableWallAvailabilityCalendar;
import ar.edu.itba.paw.util.CarAvailabilityAddressFormatter;
import ar.edu.itba.paw.util.CarAvailabilityCalendarMath;

import ar.edu.itba.paw.services.reservation.ReservationService;
/**
 * Read-side service that materialises bookable-day calendars and pricing aggregates from the
 * underlying {@code car_availability} rows.
 *
 * <p>Extracted from {@code CarAvailabilityServiceImpl} so each service stays focused: this one
 * owns "given existing rows + blocking reservations, what does the rider/owner see?", whereas
 * the original service keeps the persistence + mutation + publish-flow validations.</p>
 */
@Service
public class CarAvailabilityCalendarServiceImpl implements CarAvailabilityCalendarService {

    private final CarAvailabilityService carAvailabilityService;
    private final ReservationService reservationService;
    private final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;

    /**
     * {@code @Lazy} on the {@link CarAvailabilityService} dependency breaks the bidirectional cycle
     * with {@link CarAvailabilityServiceImpl} (which already injects this calendar service for its
     * own read-side facade methods). Architectural rule: only the owner ServiceImpl talks to the
     * matching DAO, so reads of {@code car_availability} rows are funneled through the service
     * interface here.
     */
    @Autowired
    public CarAvailabilityCalendarServiceImpl(
            @Lazy final CarAvailabilityService carAvailabilityService,
            @Lazy final ReservationService reservationService,
            final CarAvailabilityAddressFormatter carAvailabilityAddressFormatter) {
        this.carAvailabilityService = carAvailabilityService;
        this.reservationService = reservationService;
        this.carAvailabilityAddressFormatter = carAvailabilityAddressFormatter;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<AvailabilityPeriod>> getBookableWallAvailabilityPeriodsByCars(
            final Collection<Long> carIds) {
        if (carIds == null || carIds.isEmpty()) {
            return Map.of();
        }
        // Two batch reads instead of N+1: all availability rows for the requested cars and all
        // blocking reservations for the same set. A null min-end filter loads every row; do not
        // use LocalDate.MIN here — PostgreSQL rejects that year when bound as a DATE parameter.
        final List<CarAvailability> allRows = carAvailabilityService.findByCarIdsEndingOnOrAfter(
                carIds, null);
        final Map<Long, List<CarAvailability>> rowsByCarId = allRows.stream()
                .collect(Collectors.groupingBy(CarAvailability::getCarId));
        final Map<Long, List<BlockingReservationProjection>> blockingByCarId =
                reservationService.findBlockingReservationsByCarIds(carIds);
        final Map<Long, List<AvailabilityPeriod>> result = new HashMap<>(carIds.size());
        for (final Long carId : carIds) {
            if (carId == null) {
                continue;
            }
            final SortedSet<LocalDate> days = computeBookableWallDaysFromRows(
                    rowsByCarId.getOrDefault(carId, List.of()),
                    blockingByCarId.getOrDefault(carId, List.of()));
            result.put(carId, CarAvailabilityCalendarMath.mergeAdjacentWallDaysToPeriods(days));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCar(
            final long carId, final Instant now) {
        return computeBookableSegmentsForRiderDatePicker(carId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarAvailability> findEffectiveOfferedByCar(final long carId) {
        return computeEffectiveOffered(carAvailabilityService.findByCarId(carId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookableSegmentProjection> getEffectiveSegmentsForOwnerCalendarInRange(
            final long carId, final LocalDate from, final LocalDate to) {
        return computeEffectiveSegmentsForOwnerCalendar(
                carAvailabilityService.findOverlappingRangeByCar(carId, from, to));
    }

    /**
     * Rows with the same createdAt are co-created "peers" (same batch/operation) and
     * must not supersede each other — otherwise a larger peer could absorb a smaller one
     * created in the same publish form.  We therefore process rows in groups of identical
     * createdAt (newest group first).  Within a group every OFFERED row is checked
     * against days claimed by *previous* (newer) groups only; peers are independent.
     * WITHDRAWN rows participate in claiming days so that edits — which always produce
     * WITHDRAWN rows for removed chunks — correctly invalidate overlapping older OFFERED rows.
     */
    static List<CarAvailability> computeEffectiveOffered(final List<CarAvailability> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        final Map<OffsetDateTime, List<CarAvailability>> byTime = rows.stream()
                .collect(Collectors.groupingBy(CarAvailability::getCreatedAt));
        final List<OffsetDateTime> times = byTime.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        final Set<LocalDate> claimed = new HashSet<>();
        final Set<Long> effectiveIds = new HashSet<>();
        for (final OffsetDateTime time : times) {
            final List<CarAvailability> group = byTime.get(time);
            for (final CarAvailability la : group) {
                if (la.getKind() != CarAvailability.Kind.OFFERED) {
                    continue;
                }
                LocalDate d = la.getStartInclusive();
                while (!d.isAfter(la.getEndInclusive())) {
                    if (!claimed.contains(d)) {
                        effectiveIds.add(la.getId());
                        break;
                    }
                    d = d.plusDays(1);
                }
            }
            for (final CarAvailability la : group) {
                LocalDate d = la.getStartInclusive();
                while (!d.isAfter(la.getEndInclusive())) {
                    claimed.add(d);
                    d = d.plusDays(1);
                }
            }
        }
        return rows.stream()
                .filter(la -> effectiveIds.contains(la.getId()))
                .sorted(Comparator.comparing(CarAvailability::getStartInclusive))
                .collect(Collectors.toList());
    }

    private List<BookableSegmentProjection> computeEffectiveSegmentsForOwnerCalendar(
            final List<CarAvailability> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        final SortedSet<LocalDate> offeredDays = new TreeSet<>();
        for (final CarAvailability row : rows) {
            if (row.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            LocalDate d = row.getStartInclusive();
            while (!d.isAfter(row.getEndInclusive())) {
                offeredDays.add(d);
                d = d.plusDays(1);
            }
        }
        if (offeredDays.isEmpty()) {
            return List.of();
        }
        final Map<LocalDate, CarAvailability> effectiveByDay =
                indexEffectiveOfferedRowByDay(rows, offeredDays);
        final List<BookableSegmentProjection> singleDay = new ArrayList<>(offeredDays.size());
        for (final LocalDate day : offeredDays) {
            final CarAvailability eff = effectiveByDay.get(day);
            if (eff == null || eff.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            singleDay.add(new BookableSegmentProjection(
                    day, day,
                    eff.getDayPriceValue(),
                    eff.getCheckInTime(),
                    eff.getCheckOutTime(),
                    carAvailabilityAddressFormatter.formatPublicPickupLocation(eff),
                    eff.getNeighborhoodId().orElse(null),
                    eff.getId(),
                    eff.getCarId()));
        }
        return CarAvailabilityCalendarMath.mergeContiguousIdenticalProjections(singleDay);
    }

    private List<BookableSegmentProjection> computeBookableSegmentsForRiderDatePicker(
            final long carId, final Instant now) {
        final List<CarAvailability> allRows = carAvailabilityService.findByCarId(carId);
        final List<BlockingReservationProjection> blockingReservations =
                reservationService.findBlockingReservationsByCarId(carId);
        return buildRiderBookableSegments(allRows, blockingReservations, now);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> hasRiderBookableSegmentsByCarIds(
            final Collection<Long> carIds, final Instant now) {
        if (carIds == null || carIds.isEmpty()) {
            return Map.of();
        }
        final List<CarAvailability> allRows = carAvailabilityService.findByCarIdsEndingOnOrAfter(carIds, null);
        final Map<Long, List<CarAvailability>> rowsByCarId = allRows.stream()
                .collect(Collectors.groupingBy(CarAvailability::getCarId));
        final Map<Long, List<BlockingReservationProjection>> blockingByCarId =
                reservationService.findBlockingReservationsByCarIds(carIds);
        final Map<Long, Boolean> result = new HashMap<>(carIds.size());
        for (final Long carId : carIds) {
            if (carId == null) {
                continue;
            }
            final List<BookableSegmentProjection> segments = buildRiderBookableSegments(
                    rowsByCarId.getOrDefault(carId, List.of()),
                    blockingByCarId.getOrDefault(carId, List.of()),
                    now);
            result.put(carId, !segments.isEmpty());
        }
        return result;
    }

    private List<BookableSegmentProjection> buildRiderBookableSegments(
            final List<CarAvailability> allRows,
            final List<BlockingReservationProjection> blockingReservations,
            final Instant now) {
        final SortedSet<LocalDate> bookableDays = computeBookableWallDaysFromRows(allRows, blockingReservations);
        if (bookableDays.isEmpty()) {
            return List.of();
        }
        final Map<LocalDate, CarAvailability> effectiveByDay =
                indexEffectiveOfferedRowByDay(allRows, bookableDays);
        final List<BookableSegmentProjection> singleDay = new ArrayList<>(bookableDays.size());
        for (final LocalDate day : bookableDays) {
            final CarAvailability eff = effectiveByDay.get(day);
            if (eff == null || eff.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            singleDay.add(new BookableSegmentProjection(
                    day,
                    day,
                    eff.getDayPriceValue(),
                    eff.getCheckInTime(),
                    eff.getCheckOutTime(),
                    carAvailabilityAddressFormatter.formatPublicPickupLocation(eff),
                    eff.getNeighborhoodId().orElse(null),
                    eff.getId(),
                    eff.getCarId()));
        }
        final List<BookableSegmentProjection> merged =
                CarAvailabilityCalendarMath.mergeContiguousIdenticalProjections(singleDay);
        final int leadHours = reservationService.getConfiguredPickupLeadHours();
        final Instant minPickupExclusive = now.plus(leadHours, ChronoUnit.HOURS);
        return CarAvailabilityCalendarMath.clipSegmentsByPickupLead(merged, AppTimezone.WALL_ZONE, minPickupExclusive);
    }

    /**
     * Builds the "effective availability row for this day" map by walking all rows in
     * {@code createdAt} desc (ties broken by id desc) and claiming each in-range day for the
     * first row that covers it — same precedence rule {@code findEffectiveForDayByCar} applies
     * one day at a time, just done once in memory for every {@code wantedDays} entry.
     */
    private static Map<LocalDate, CarAvailability> indexEffectiveOfferedRowByDay(
            final List<CarAvailability> allRows, final SortedSet<LocalDate> wantedDays) {
        if (wantedDays.isEmpty() || allRows.isEmpty()) {
            return Map.of();
        }
        final List<CarAvailability> ordered = new ArrayList<>(allRows);
        // Null-safe ordering so unit tests (which build CarAvailability without auditing
        // populating createdAt) still get a stable "most recent createdAt wins, ties broken by id"
        // resolution. nullsLast on the descending sort means a null createdAt loses to any populated
        // one — identical to how a freshly-inserted row would lose to a previously-persisted row.
        ordered.sort(Comparator
                .comparing(CarAvailability::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CarAvailability::getId, Comparator.reverseOrder()));
        final Map<LocalDate, CarAvailability> effective = new HashMap<>(wantedDays.size());
        for (final CarAvailability row : ordered) {
            final LocalDate start = row.getStartInclusive();
            final LocalDate end = row.getEndInclusive();
            for (final LocalDate day : wantedDays) {
                if (day.isBefore(start) || day.isAfter(end)) {
                    continue;
                }
                effective.putIfAbsent(day, row);
            }
            if (effective.size() == wantedDays.size()) {
                break;
            }
        }
        return effective;
    }

    /**
     * Shared core of {@link #computeBookableWallDaysByCarInternal} and the batch path: builds the
     * bookable wall-day set from already-fetched OFFERED availability rows and blocking
     * reservations. Lives here so the per-car and per-batch paths cannot drift.
     *
     * <p>In-memory day expansion is intentional and bounded: each availability row is a single
     * period (owner calendar is month-scoped at the HTTP layer; bookable checks operate on the
     * car's offered rows + blocking projections, not the global catalogue). Global browse/search
     * pagination remains in SQL.</p>
     */
    private static SortedSet<LocalDate> computeBookableWallDaysFromRows(
            final List<CarAvailability> rows, final List<BlockingReservationProjection> blockingReservations) {
        final ZoneId wall = AppTimezone.WALL_ZONE;
        final SortedSet<LocalDate> days = new TreeSet<>();
        for (final CarAvailability la : rows) {
            if (la.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            LocalDate d = la.getStartInclusive();
            while (!d.isAfter(la.getEndInclusive())) {
                days.add(d);
                d = d.plusDays(1);
            }
        }
        for (final BlockingReservationProjection r : blockingReservations) {
            LocalDate d = r.getStartDate().toInstant().atZone(wall).toLocalDate();
            final LocalDate until = r.getEndDate().toInstant().atZone(wall).toLocalDate();
            while (!d.isAfter(until)) {
                days.remove(d);
                d = d.plusDays(1);
            }
        }
        return days;
    }
}
