package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
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
public final class CarAvailabilityCalendarServiceImpl implements CarAvailabilityCalendarService {

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
    public List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsByCar(final long carId) {
        return CarAvailabilityCalendarMath.mergeAdjacentWallDaysToPeriods(computeBookableWallDaysByCar(carId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsForRiderDatePickerByCar(
            final long carId,
            final LocalTime checkInTime,
            final Instant now) {
        final List<AvailabilityPeriod> bookable = getBookableWallAvailabilityPeriodsByCar(carId);
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(bookable);
        final int leadHours = reservationService.getConfiguredPickupLeadHours();
        final Instant minPickupExclusive = now.plus(leadHours, ChronoUnit.HOURS);
        return BookableWallAvailabilityCalendar.clipPeriodsToMinPickupInstant(
                merged, checkInTime, AppTimezone.WALL_ZONE, minPickupExclusive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCar(
            final long carId, final Instant now) {
        return computeBookableSegmentsForRiderDatePicker(carId, now, /* excludingReservationId */ null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCarExcluding(
            final long carId, final Instant now, final long excludingReservationId) {
        return computeBookableSegmentsForRiderDatePicker(carId, now, excludingReservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookableSegmentProjection> getAllEffectiveSegmentsForOwnerCalendar(final long carId) {
        return findEffectiveOfferedByCar(carId).stream()
                .map(ca -> new BookableSegmentProjection(
                        ca.getStartInclusive(),
                        ca.getEndInclusive(),
                        ca.getDayPriceValue(),
                        ca.getCheckInTime(),
                        ca.getCheckOutTime(),
                        carAvailabilityAddressFormatter.formatPublicPickupLocation(ca),
                        ca.getNeighborhoodId().orElse(null)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarAvailability> findEffectiveOfferedByCar(final long carId) {
        final List<CarAvailability> all = carAvailabilityService.findByCarId(carId);
        if (all.isEmpty()) {
            return List.of();
        }
        // Rows with the same createdAt are co-created "peers" (same batch/operation) and
        // must not supersede each other — otherwise a larger peer could absorb a smaller one
        // created in the same publish form.  We therefore process rows in groups of identical
        // createdAt (newest group first).  Within a group every OFFERED row is checked
        // against days claimed by *previous* (newer) groups only; peers are independent.
        // WITHDRAWN rows participate in claiming days so that edits — which always produce
        // WITHDRAWN rows for removed chunks — correctly invalidate overlapping older OFFERED rows.
        final Map<OffsetDateTime, List<CarAvailability>> byTime = all.stream()
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
        return all.stream()
                .filter(la -> effectiveIds.contains(la.getId()))
                .sorted(Comparator.comparing(CarAvailability::getStartInclusive))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveMinEffectiveDayPriceByCar(final long carId, final BigDecimal defaultPrice) {
        final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);
        BigDecimal min = defaultPrice;
        for (final CarAvailability la : carAvailabilityService.findByCarId(carId)) {
            if (la.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            if (la.getEndInclusive().isBefore(today)) {
                continue;
            }
            final BigDecimal periodPrice = la.getDayPriceValue();
            if (periodPrice != null && (min == null || periodPrice.compareTo(min) < 0)) {
                min = periodPrice;
            }
        }
        return min;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCarPriceVariableByCar(final long carId, final BigDecimal defaultPrice) {
        final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);
        for (final CarAvailability la : carAvailabilityService.findByCarId(carId)) {
            if (la.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            if (la.getEndInclusive().isBefore(today)) {
                continue;
            }
            final BigDecimal periodPrice = la.getDayPriceValue();
            if (periodPrice != null && defaultPrice != null && periodPrice.compareTo(defaultPrice) != 0) {
                return true;
            }
        }
        return false;
    }

    private List<BookableSegmentProjection> computeBookableSegmentsForRiderDatePicker(
            final long carId, final Instant now, final Long excludingReservationIdOrNull) {
        final SortedSet<LocalDate> bookableDays = excludingReservationIdOrNull == null
                ? computeBookableWallDaysByCar(carId)
                : computeBookableWallDaysByCarExcluding(carId, excludingReservationIdOrNull);
        if (bookableDays.isEmpty()) {
            return List.of();
        }
        final List<BookableSegmentProjection> singleDay = new ArrayList<>(bookableDays.size());
        for (final LocalDate day : bookableDays) {
            final Optional<CarAvailability> effOpt = carAvailabilityService.findEffectiveForDayByCar(carId, day);
            if (effOpt.isEmpty() || effOpt.get().getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            final CarAvailability eff = effOpt.get();
            singleDay.add(new BookableSegmentProjection(
                    day,
                    day,
                    eff.getDayPriceValue(),
                    eff.getCheckInTime(),
                    eff.getCheckOutTime(),
                    carAvailabilityAddressFormatter.formatPublicPickupLocation(eff),
                    eff.getNeighborhoodId().orElse(null)));
        }
        final List<BookableSegmentProjection> merged =
                CarAvailabilityCalendarMath.mergeContiguousIdenticalProjections(singleDay);
        final int leadHours = reservationService.getConfiguredPickupLeadHours();
        final Instant minPickupExclusive = now.plus(leadHours, ChronoUnit.HOURS);
        return CarAvailabilityCalendarMath.clipSegmentsByPickupLead(merged, AppTimezone.WALL_ZONE, minPickupExclusive);
    }

    private SortedSet<LocalDate> computeBookableWallDaysByCar(final long carId) {
        return computeBookableWallDaysByCarInternal(
                carId, reservationService.findBlockingReservationsByCarId(carId));
    }

    private SortedSet<LocalDate> computeBookableWallDaysByCarExcluding(
            final long carId, final long excludingReservationId) {
        return computeBookableWallDaysByCarInternal(
                carId, reservationService.findBlockingReservationsByCarIdExcluding(carId, excludingReservationId));
    }

    private SortedSet<LocalDate> computeBookableWallDaysByCarInternal(
            final long carId, final List<Reservation> blockingReservations) {
        final ZoneId wall = AppTimezone.WALL_ZONE;
        final SortedSet<LocalDate> days = new TreeSet<>();
        for (final CarAvailability la : carAvailabilityService.findByCarId(carId)) {
            if (la.getKind() != CarAvailability.Kind.OFFERED) {
                continue;
            }
            LocalDate d = la.getStartInclusive();
            while (!d.isAfter(la.getEndInclusive())) {
                days.add(d);
                d = d.plusDays(1);
            }
        }
        for (final Reservation r : blockingReservations) {
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
