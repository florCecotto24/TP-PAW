package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.BookableWallAvailabilityCalendar;
import ar.edu.itba.paw.models.util.time.RiderPickupLeadTime;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Pass-through to {@link ListingAvailabilityDao}; joins the caller's transaction when one is active. */
@Service
public final class ListingAvailabilityServiceImpl implements ListingAvailabilityService {

    private final ListingAvailabilityDao listingAvailabilityDao;
    private final ReservationService reservationService;
    private final CarService carService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ListingAvailabilityPolicy listingAvailabilityPolicy;
    private final ListingAddressFormatter listingAddressFormatter;

    @Autowired
    public ListingAvailabilityServiceImpl(
            final ListingAvailabilityDao listingAvailabilityDao,
            @Lazy final ReservationService reservationService,
            @Lazy final CarService carService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final ListingAvailabilityPolicy listingAvailabilityPolicy,
            final ListingAddressFormatter listingAddressFormatter) {
        this.listingAvailabilityDao = listingAvailabilityDao;
        this.reservationService = reservationService;
        this.carService = carService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.listingAvailabilityPolicy = listingAvailabilityPolicy;
        this.listingAddressFormatter = listingAddressFormatter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingAvailability> findById(final long availabilityId) {
        return listingAvailabilityDao.findById(availabilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingAvailability> findEffectiveForDayByCar(final long carId, final LocalDate day) {
        return listingAvailabilityDao.findEffectiveForDayByCar(carId, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findOverlappingRangeByCar(final long carId, final LocalDate from, final LocalDate to) {
        return listingAvailabilityDao.findOverlappingRangeByCar(carId, from, to);
    }

    @Override
    @Transactional
    public List<ListingAvailability> createCarAvailabilityPeriods(
            final long carId,
            final BigDecimal dayPrice,
            final String street,
            final String number,
            final Long neighborhoodId,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> periods,
            final List<BigDecimal> periodPrices,
            final int minimumRentalDays) {
        if (carService.isModelPendingValidation(carId)) {
            throw new CarValidationException(MessageKeys.LISTING_CREATE_MODEL_PENDING);
        }
        validateMinimumRentalDaysAgainstPeriods(minimumRentalDays, periods);
        final List<ListingAvailability> result = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            final AvailabilityPeriod period = periods.get(i);
            final BigDecimal periodPrice = (periodPrices != null && i < periodPrices.size() && periodPrices.get(i) != null)
                    ? periodPrices.get(i)
                    : dayPrice;
            final ListingAvailability row = listingAvailabilityDao.createFullForCar(
                    carId,
                    period.getStartInclusive(),
                    period.getEndInclusive(),
                    periodPrice,
                    street,
                    number,
                    neighborhoodId,
                    checkInTime,
                    checkOutTime,
                    ListingAvailability.Kind.OFFERED);
            result.add(row);
        }
        carService.updateMinimumRentalDays(carId, minimumRentalDays);
        return result;
    }

    @Override
    @Transactional
    public void updateMinimumRentalDays(final long carId, final int minDays) {
        final List<AvailabilityPeriod> periods = findEffectiveOfferedByCar(carId).stream()
                .map(la -> new AvailabilityPeriod(la.getStartInclusive(), la.getEndInclusive()))
                .collect(Collectors.toList());
        if (!periods.isEmpty()) {
            validateMinimumRentalDaysAgainstPeriods(minDays, periods);
        }
        carService.updateMinimumRentalDays(carId, minDays);
    }

    private static void validateMinimumRentalDaysAgainstPeriods(
            final int minDays, final List<AvailabilityPeriod> periods) {
        for (final AvailabilityPeriod period : periods) {
            final long periodLength = ChronoUnit.DAYS.between(period.getStartInclusive(), period.getEndInclusive()) + 1;
            if (minDays > periodLength) {
                throw new CarValidationException(
                        MessageKeys.LISTING_MIN_RENTAL_DAYS_EXCEEDS_PERIOD,
                        new Object[]{minDays});
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findByCarId(final long carId) {
        return listingAvailabilityDao.findByCarId(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findEffectiveOfferedByCar(final long carId) {
        final List<ListingAvailability> all = listingAvailabilityDao.findByCarId(carId);
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
        final Map<OffsetDateTime, List<ListingAvailability>> byTime = all.stream()
                .collect(Collectors.groupingBy(ListingAvailability::getCreatedAt));
        final List<OffsetDateTime> times = byTime.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        final Set<LocalDate> claimed = new HashSet<>();
        final Set<Long> effectiveIds = new HashSet<>();
        for (final OffsetDateTime time : times) {
            final List<ListingAvailability> group = byTime.get(time);
            // First pass: identify OFFERED rows in this group that have at least one day
            // not yet claimed by any newer group.
            for (final ListingAvailability la : group) {
                if (la.getKind() != ListingAvailability.Kind.OFFERED) {
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
            // Second pass: claim every day in this group (OFFERED and WITHDRAWN alike)
            // so that older groups cannot reuse these days.
            for (final ListingAvailability la : group) {
                LocalDate d = la.getStartInclusive();
                while (!d.isAfter(la.getEndInclusive())) {
                    claimed.add(d);
                    d = d.plusDays(1);
                }
            }
        }
        return all.stream()
                .filter(la -> effectiveIds.contains(la.getId()))
                .sorted(Comparator.comparing(ListingAvailability::getStartInclusive))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findByCarIdsEndingOnOrAfter(
            final Collection<Long> carIds,
            final LocalDate minEndDate) {
        return listingAvailabilityDao.findByCarIdsEndingOnOrAfter(carIds, minEndDate);
    }

    @Override
    @Transactional
    public void deleteByCarId(final long carId) {
        listingAvailabilityDao.deleteByCarId(carId);
    }

    @Override
    @Transactional
    public ListingAvailability applyOwnerEditByCar(
            final long carId,
            final LocalDate oldStartInclusive,
            final LocalDate oldEndInclusive,
            final LocalDate newStartInclusive,
            final LocalDate newEndInclusive,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final Long neighborhoodId,
            final LocalTime checkInTime,
            final LocalTime checkOutTime) {
        Objects.requireNonNull(oldStartInclusive, "oldStartInclusive");
        Objects.requireNonNull(oldEndInclusive, "oldEndInclusive");
        Objects.requireNonNull(newStartInclusive, "newStartInclusive");
        Objects.requireNonNull(newEndInclusive, "newEndInclusive");
        if (newEndInclusive.isBefore(newStartInclusive)) {
            throw new IllegalArgumentException("newEndInclusive before newStartInclusive");
        }

        final List<DateRange> removed = subtractDayRange(oldStartInclusive, oldEndInclusive,
                newStartInclusive, newEndInclusive);

        rejectIfReservationsOverlapAnyChunkByCar(carId, removed, MessageKeys.LISTING_AVAILABILITY_EDIT_CONFLICT);

        final ListingAvailability offered = listingAvailabilityDao.createFullForCar(
                carId, newStartInclusive, newEndInclusive, dayPrice,
                startPointStreet, startPointNumber, neighborhoodId,
                checkInTime, checkOutTime, ListingAvailability.Kind.OFFERED);

        for (final DateRange chunk : removed) {
            listingAvailabilityDao.createFullForCar(
                    carId, chunk.start, chunk.end, dayPrice,
                    startPointStreet, startPointNumber, neighborhoodId,
                    checkInTime, checkOutTime, ListingAvailability.Kind.WITHDRAWN);
        }

        return offered;
    }

    @Override
    @Transactional
    public ListingAvailability applyOwnerWithdrawByCar(final long carId, final long availabilityId) {
        final ListingAvailability target = listingAvailabilityDao.findById(availabilityId)
                .orElseThrow(() -> new CarValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_FOUND));
        if (target.getCarId() != carId) {
            throw new CarValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_OWNED);
        }
        if (target.getKind() != ListingAvailability.Kind.OFFERED) {
            throw new CarValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_OFFERED);
        }

        final List<DateRange> withdrawnChunks = List.of(
                new DateRange(target.getStartInclusive(), target.getEndInclusive()));
        rejectIfReservationsOverlapAnyChunkByCar(carId, withdrawnChunks,
                MessageKeys.LISTING_AVAILABILITY_WITHDRAW_CONFLICT);

        return listingAvailabilityDao.createFullForCar(
                carId,
                target.getStartInclusive(),
                target.getEndInclusive(),
                target.getDayPriceValue(),
                target.getStartPointStreet(),
                target.getStartPointNumber().orElse(null),
                target.getNeighborhoodId().orElse(null),
                target.getCheckInTime(),
                target.getCheckOutTime(),
                ListingAvailability.Kind.WITHDRAWN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsByCar(final long carId) {
        return mergeAdjacentWallDaysToPeriods(computeBookableWallDaysByCar(carId));
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
        final SortedSet<LocalDate> bookableDays = computeBookableWallDaysByCar(carId);
        if (bookableDays.isEmpty()) {
            return List.of();
        }
        final List<BookableSegmentProjection> singleDay = new ArrayList<>(bookableDays.size());
        for (final LocalDate day : bookableDays) {
            final Optional<ListingAvailability> effOpt = listingAvailabilityDao.findEffectiveForDayByCar(carId, day);
            if (effOpt.isEmpty() || effOpt.get().getKind() != ListingAvailability.Kind.OFFERED) {
                continue;
            }
            final ListingAvailability eff = effOpt.get();
            singleDay.add(new BookableSegmentProjection(
                    day,
                    day,
                    eff.getDayPriceValue(),
                    eff.getCheckInTime(),
                    eff.getCheckOutTime(),
                    listingAddressFormatter.formatPublicPickupLocation(eff),
                    eff.getNeighborhoodId().orElse(null)));
        }
        final List<BookableSegmentProjection> merged = mergeContiguousIdenticalProjections(singleDay);
        final int leadHours = reservationService.getConfiguredPickupLeadHours();
        final Instant minPickupExclusive = now.plus(leadHours, ChronoUnit.HOURS);
        return clipSegmentsByPickupLead(merged, AppTimezone.WALL_ZONE, minPickupExclusive);
    }

    private SortedSet<LocalDate> computeBookableWallDaysByCar(final long carId) {
        final ZoneId wall = AppTimezone.WALL_ZONE;
        final SortedSet<LocalDate> days = new TreeSet<>();
        for (final ListingAvailability la : listingAvailabilityDao.findByCarId(carId)) {
            if (la.getKind() != ListingAvailability.Kind.OFFERED) {
                continue;
            }
            LocalDate d = la.getStartInclusive();
            while (!d.isAfter(la.getEndInclusive())) {
                days.add(d);
                d = d.plusDays(1);
            }
        }
        for (final Reservation r : reservationService.findBlockingReservationsByCarId(carId)) {
            LocalDate d = r.getStartDate().toInstant().atZone(wall).toLocalDate();
            final LocalDate until = r.getEndDate().toInstant().atZone(wall).toLocalDate();
            while (!d.isAfter(until)) {
                days.remove(d);
                d = d.plusDays(1);
            }
        }
        return days;
    }

    private static List<BookableSegmentProjection> mergeContiguousIdenticalProjections(
            final List<BookableSegmentProjection> singleDay) {
        if (singleDay.isEmpty()) {
            return List.of();
        }
        final List<BookableSegmentProjection> out = new ArrayList<>();
        BookableSegmentProjection cur = singleDay.get(0);
        for (int i = 1; i < singleDay.size(); i++) {
            final BookableSegmentProjection next = singleDay.get(i);
            final boolean contiguous = next.getFrom().equals(cur.getTo().plusDays(1));
            final boolean sameProjection = Objects.equals(cur.getDayPrice(), next.getDayPrice())
                    && Objects.equals(cur.getCheckInTime(), next.getCheckInTime())
                    && Objects.equals(cur.getCheckOutTime(), next.getCheckOutTime())
                    && cur.getPublicLocation().equals(next.getPublicLocation())
                    && Objects.equals(cur.getNeighborhoodId(), next.getNeighborhoodId());
            if (contiguous && sameProjection) {
                cur = new BookableSegmentProjection(
                        cur.getFrom(), next.getTo(),
                        cur.getDayPrice(), cur.getCheckInTime(),
                        cur.getCheckOutTime(), cur.getPublicLocation(),
                        cur.getNeighborhoodId());
            } else {
                out.add(cur);
                cur = next;
            }
        }
        out.add(cur);
        return List.copyOf(out);
    }

    private static List<BookableSegmentProjection> clipSegmentsByPickupLead(
            final List<BookableSegmentProjection> merged,
            final ZoneId wallZone,
            final Instant minPickupExclusive) {
        if (merged.isEmpty()) {
            return List.of();
        }
        final List<BookableSegmentProjection> clipped = new ArrayList<>();
        for (final BookableSegmentProjection seg : merged) {
            final LocalTime pickup = seg.getCheckInTime() != null
                    ? seg.getCheckInTime()
                    : ListingAvailability.DEFAULT_CHECK_IN_TIME;
            LocalDate d = seg.getFrom();
            while (!d.isAfter(seg.getTo())) {
                final Instant pickupInstant = ZonedDateTime.of(d, pickup, wallZone).toInstant();
                if (pickupInstant.isAfter(minPickupExclusive)) {
                    if (d.equals(seg.getFrom())) {
                        clipped.add(seg);
                    } else {
                        clipped.add(new BookableSegmentProjection(
                                d, seg.getTo(),
                                seg.getDayPrice(), seg.getCheckInTime(),
                                seg.getCheckOutTime(), seg.getPublicLocation(),
                                seg.getNeighborhoodId()));
                    }
                    break;
                }
                d = d.plusDays(1);
            }
        }
        return List.copyOf(clipped);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveMinEffectiveDayPriceByCar(final long carId, final BigDecimal defaultPrice) {
        final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);
        BigDecimal min = defaultPrice;
        for (final ListingAvailability la : listingAvailabilityDao.findByCarId(carId)) {
            if (la.getKind() != ListingAvailability.Kind.OFFERED) {
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
        for (final ListingAvailability la : listingAvailabilityDao.findByCarId(carId)) {
            if (la.getKind() != ListingAvailability.Kind.OFFERED) {
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

    private static List<AvailabilityPeriod> mergeAdjacentWallDaysToPeriods(final SortedSet<LocalDate> days) {
        if (days.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityPeriod> out = new ArrayList<>();
        LocalDate segStart = null;
        LocalDate prev = null;
        for (final LocalDate d : days) {
            if (segStart == null) {
                segStart = d;
                prev = d;
            } else if (d.equals(prev.plusDays(1))) {
                prev = d;
            } else {
                out.add(new AvailabilityPeriod(segStart, prev));
                segStart = d;
                prev = d;
            }
        }
        out.add(new AvailabilityPeriod(segStart, prev));
        return List.copyOf(out);
    }

    private void rejectIfReservationsOverlapAnyChunkByCar(
            final long carId, final List<DateRange> chunks, final String conflictMessageKey) {
        if (chunks.isEmpty()) {
            return;
        }
        final ZoneId wall = AppTimezone.WALL_ZONE;
        final OffsetDateTime fromUtc = chunks.get(0).start.atStartOfDay(wall).toOffsetDateTime();
        final OffsetDateTime toUtc = chunks.get(chunks.size() - 1).end.plusDays(1).atStartOfDay(wall).toOffsetDateTime();
        final List<Reservation> blocking =
                reservationService.findBlockingReservationsByCarIdInRange(carId, fromUtc, toUtc);
        for (final Reservation r : blocking) {
            final LocalDate rStart = r.getStartDate().atZoneSameInstant(wall).toLocalDate();
            final LocalDate rEnd = r.getEndDate().atZoneSameInstant(wall).toLocalDate();
            for (final DateRange chunk : chunks) {
                if (!rEnd.isBefore(chunk.start) && !rStart.isAfter(chunk.end)) {
                    throw new ReservationConflictException(conflictMessageKey);
                }
            }
        }
    }

    private static List<DateRange> subtractDayRange(
            final LocalDate oldStart, final LocalDate oldEnd,
            final LocalDate newStart, final LocalDate newEnd) {
        final LocalDate overlapStart = oldStart.isAfter(newStart) ? oldStart : newStart;
        final LocalDate overlapEnd = oldEnd.isBefore(newEnd) ? oldEnd : newEnd;
        if (overlapStart.isAfter(overlapEnd)) {
            return List.of(new DateRange(oldStart, oldEnd));
        }
        final List<DateRange> out = new ArrayList<>(2);
        if (oldStart.isBefore(overlapStart)) {
            out.add(new DateRange(oldStart, overlapStart.minusDays(1)));
        }
        if (oldEnd.isAfter(overlapEnd)) {
            out.add(new DateRange(overlapEnd.plusDays(1), oldEnd));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingAvailability> findMostRecentByCarId(final long carId) {
        return listingAvailabilityDao.findByCarId(carId).stream()
                .max(java.util.Comparator.comparing(ListingAvailability::getCreatedAt));
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate getPublicationMinAvailabilityFirstWallDay(
            final LocalTime checkInTime, final Instant now) {
        final LocalTime pickup = checkInTime != null ? checkInTime : ListingAvailability.DEFAULT_CHECK_IN_TIME;
        return RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup,
                AppTimezone.WALL_ZONE,
                now,
                reservationTimingPolicy.getPickupLeadHours());
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePublicationAvailabilityRiderLead(
            final List<AvailabilityPeriod> periods, final LocalTime checkInTime, final Instant now) {
        if (periods == null || periods.isEmpty()) {
            return;
        }
        final int pickupLeadHours = reservationTimingPolicy.getPickupLeadHours();
        final LocalTime pickup = checkInTime != null ? checkInTime : ListingAvailability.DEFAULT_CHECK_IN_TIME;
        final LocalDate minStart = RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup, AppTimezone.WALL_ZONE, now, pickupLeadHours);
        for (int i = 0; i < periods.size(); i++) {
            final LocalDate from = periods.get(i).getStartInclusive();
            if (from.isBefore(minStart)) {
                throw new AvailabilityRiderLeadViolationException(
                        i, "validation.availabilityRow.from.riderLeadTime", minStart, pickupLeadHours);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEditAvailabilityRiderLead(
            final List<AvailabilityPeriod> periods,
            final LocalTime checkInTime,
            final Instant now,
            final LocalDate originalStartInclusive) {
        if (periods == null || periods.isEmpty()) {
            return;
        }
        // If the start date is unchanged the lead-time requirement was already satisfied
        // when the period was originally published (it may even be in progress), so
        // re-validating it would incorrectly block price / end-date edits.
        final LocalDate newStart = periods.get(0).getStartInclusive();
        if (newStart.equals(originalStartInclusive)) {
            return;
        }
        validatePublicationAvailabilityRiderLead(periods, checkInTime, now);
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePublicationAvailabilityAgainstWallCalendar(final List<AvailabilityPeriod> periods) {
        listingAvailabilityPolicy.validateAvailabilityWithinPublishHorizon(
                LocalDate.now(AppTimezone.WALL_ZONE), periods);
    }

    @Override
    public int getConfiguredMaxAvailabilityForwardWallDays() {
        return listingAvailabilityPolicy.getMaxAvailabilityForwardWallDays();
    }

    private static final class DateRange {
        final LocalDate start;
        final LocalDate end;

        DateRange(final LocalDate start, final LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
