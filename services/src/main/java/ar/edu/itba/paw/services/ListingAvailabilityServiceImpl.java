package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.util.BookableWallAvailabilityCalendar;
import ar.edu.itba.paw.models.util.RiderPickupLeadTime;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/** Pass-through to {@link ListingAvailabilityDao}; joins the caller's transaction when one is active. */
@Service
public final class ListingAvailabilityServiceImpl implements ListingAvailabilityService {

    private final ListingAvailabilityDao listingAvailabilityDao;
    private final ReservationService reservationService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ListingAvailabilityPolicy listingAvailabilityPolicy;

    @Autowired
    public ListingAvailabilityServiceImpl(
            final ListingAvailabilityDao listingAvailabilityDao,
            @Lazy final ReservationService reservationService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final ListingAvailabilityPolicy listingAvailabilityPolicy) {
        this.listingAvailabilityDao = listingAvailabilityDao;
        this.reservationService = reservationService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.listingAvailabilityPolicy = listingAvailabilityPolicy;
    }

    @Override
    @Transactional
    public ListingAvailability create(final long listingId, final LocalDate startInclusive,
            final LocalDate endInclusive, final BigDecimal dayPrice) {
        return listingAvailabilityDao.create(listingId, startInclusive, endInclusive, dayPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingAvailability> findById(final long availabilityId) {
        return listingAvailabilityDao.findById(availabilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findByListingId(final long listingId) {
        return listingAvailabilityDao.findByListingId(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findByListingIdsEndingOnOrAfter(
            final Collection<Long> listingIds,
            final LocalDate minEndDate) {
        return listingAvailabilityDao.findByListingIdsEndingOnOrAfter(listingIds, minEndDate);
    }

    @Override
    @Transactional
    public void deleteByListingId(final long listingId) {
        listingAvailabilityDao.deleteByListingId(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingAvailability> findEffectiveForDay(final long listingId, final LocalDate day) {
        return listingAvailabilityDao.findEffectiveForDay(listingId, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findOverlappingRange(final long listingId, final LocalDate from, final LocalDate to) {
        return listingAvailabilityDao.findOverlappingRange(listingId, from, to);
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
            final List<BigDecimal> periodPrices) {
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
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findByCarId(final long carId) {
        return listingAvailabilityDao.findByCarId(carId);
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
    public ListingAvailability applyOwnerEdit(
            final long listingId,
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

        rejectIfReservationsOverlapAnyChunk(listingId, removed, MessageKeys.LISTING_AVAILABILITY_EDIT_CONFLICT);

        final ListingAvailability offered = listingAvailabilityDao.createFull(
                listingId, newStartInclusive, newEndInclusive, dayPrice,
                startPointStreet, startPointNumber, neighborhoodId,
                checkInTime, checkOutTime, ListingAvailability.Kind.OFFERED);

        for (final DateRange chunk : removed) {
            listingAvailabilityDao.createFull(
                    listingId, chunk.start, chunk.end, dayPrice,
                    startPointStreet, startPointNumber, neighborhoodId,
                    checkInTime, checkOutTime, ListingAvailability.Kind.WITHDRAWN);
        }

        return offered;
    }

    @Override
    @Transactional
    public ListingAvailability applyOwnerWithdrawAvailability(final long listingId, final long availabilityId) {
        final ListingAvailability target = listingAvailabilityDao.findById(availabilityId)
                .orElseThrow(() -> new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_FOUND));
        if (target.getListingId() != listingId) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_OWNED);
        }
        if (target.getKind() != ListingAvailability.Kind.OFFERED) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_OFFERED);
        }

        final List<DateRange> withdrawnChunks = List.of(
                new DateRange(target.getStartInclusive(), target.getEndInclusive()));
        rejectIfReservationsOverlapAnyChunk(listingId, withdrawnChunks,
                MessageKeys.LISTING_AVAILABILITY_WITHDRAW_CONFLICT);

        return listingAvailabilityDao.createFull(
                listingId,
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
                .orElseThrow(() -> new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_FOUND));
        if (target.getCarId() != carId) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_OWNED);
        }
        if (target.getKind() != ListingAvailability.Kind.OFFERED) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_NOT_OFFERED);
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
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
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
        return mergeAdjacentWallDaysToPeriods(days);
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
                merged, checkInTime, AvailabilityPeriod.WALL_ZONE, minPickupExclusive);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal resolveMinEffectiveDayPriceByCar(final long carId, final BigDecimal defaultPrice) {
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
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
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
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

    private void rejectIfReservationsOverlapAnyChunk(
            final long listingId, final List<DateRange> chunks, final String conflictMessageKey) {
        if (chunks.isEmpty()) {
            return;
        }
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        final OffsetDateTime fromUtc = chunks.get(0).start.atStartOfDay(wall).toOffsetDateTime();
        final OffsetDateTime toUtc = chunks.get(chunks.size() - 1).end.plusDays(1).atStartOfDay(wall).toOffsetDateTime();
        final List<Reservation> blocking =
                reservationService.findBlockingReservationsByListingIdInRange(listingId, fromUtc, toUtc);
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

    private void rejectIfReservationsOverlapAnyChunkByCar(
            final long carId, final List<DateRange> chunks, final String conflictMessageKey) {
        if (chunks.isEmpty()) {
            return;
        }
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
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
        final LocalTime pickup = checkInTime != null ? checkInTime : Listing.DEFAULT_CHECK_IN_TIME;
        return RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup,
                AvailabilityPeriod.WALL_ZONE,
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
        final LocalTime pickup = checkInTime != null ? checkInTime : Listing.DEFAULT_CHECK_IN_TIME;
        final LocalDate minStart = RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup, AvailabilityPeriod.WALL_ZONE, now, pickupLeadHours);
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
    public void validatePublicationAvailabilityAgainstWallCalendar(final List<AvailabilityPeriod> periods) {
        listingAvailabilityPolicy.validateAvailabilityWithinPublishHorizon(
                LocalDate.now(AvailabilityPeriod.WALL_ZONE), periods);
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
