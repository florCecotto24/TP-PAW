package ar.edu.itba.paw.services.car;


import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.AvailabilityCreateInput;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.dto.reservation.BlockingReservationProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.RiderPickupLeadTime;
import ar.edu.itba.paw.persistence.car.CarAvailabilityDao;
import ar.edu.itba.paw.policy.CarAvailabilityPolicy;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.CarAvailabilityCalendarMath;
import ar.edu.itba.paw.util.CarAvailabilityCalendarMath.DateRange;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserReadinessService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Owns car_availability persistence + owner-driven mutations (create/edit/withdraw/listing) +
 * publish-flow validations (rider lead, wall calendar, minimum rental days).
 *
 * <p>The bookable-day calendar derivations + per-car pricing aggregates live in
 * {@link CarAvailabilityCalendarServiceImpl}; the corresponding methods are kept on this
 * service's interface (back-compat) and delegated through.</p>
 */
@Service
public class CarAvailabilityServiceImpl implements CarAvailabilityService {

    private final CarAvailabilityDao carAvailabilityDao;
    private final ReservationService reservationService;
    private final CarService carService;
    private final UserService userService;
    private final UserReadinessService userReadinessService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final CarAvailabilityPolicy carAvailabilityPolicy;
    private final CarAvailabilityCalendarService carAvailabilityCalendarService;

    @Autowired
    public CarAvailabilityServiceImpl(
            final CarAvailabilityDao carAvailabilityDao,
            @Lazy final ReservationService reservationService,
            @Lazy final CarService carService,
            final UserService userService,
            final UserReadinessService userReadinessService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final CarAvailabilityPolicy carAvailabilityPolicy,
            @Lazy final CarAvailabilityCalendarService carAvailabilityCalendarService) {
        this.carAvailabilityDao = carAvailabilityDao;
        this.reservationService = reservationService;
        this.carService = carService;
        this.userService = userService;
        this.userReadinessService = userReadinessService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.carAvailabilityPolicy = carAvailabilityPolicy;
        this.carAvailabilityCalendarService = carAvailabilityCalendarService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarAvailability> findById(final long availabilityId) {
        return carAvailabilityDao.findById(availabilityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarAvailability> findByIdForCar(final long carId, final long availabilityId) {
        return carAvailabilityDao.findById(availabilityId)
                .filter(av -> av.getCarId() == carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarAvailability> findEffectiveForDayByCar(final long carId, final LocalDate day) {
        return carAvailabilityDao.findEffectiveForDayByCar(carId, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarAvailability> findOverlappingRangeByCar(final long carId, final LocalDate from, final LocalDate to) {
        return carAvailabilityDao.findOverlappingRangeByCar(carId, from, to);
    }

    private List<CarAvailability> createCarAvailabilityPeriods(
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
        // Defense-in-depth: opening a new availability window makes the car bookable again. The UI hides
        // the entry point for blocked owners (myCarDetail.jsp), but a direct POST must still be rejected.
        requireOwnerOfCarNotBlocked(carId);
        if (carService.isModelPendingValidation(carId)) {
            throw new CarValidationException(MessageKeys.CAR_CREATE_MODEL_PENDING);
        }
        CarAvailabilityCalendarMath.validateMinimumRentalDaysAgainstPeriods(minimumRentalDays, periods);
        final List<CarAvailability> result = new ArrayList<>();
        for (int i = 0; i < periods.size(); i++) {
            final AvailabilityPeriod period = periods.get(i);
            final BigDecimal periodPrice = (periodPrices != null && i < periodPrices.size() && periodPrices.get(i) != null)
                    ? periodPrices.get(i)
                    : dayPrice;
            final CarAvailability row = carAvailabilityDao.createFullForCar(
                    carId,
                    period.getStartInclusive(),
                    period.getEndInclusive(),
                    periodPrice,
                    street,
                    number,
                    neighborhoodId,
                    checkInTime,
                    checkOutTime,
                    CarAvailability.Kind.OFFERED);
            result.add(row);
        }
        carService.updateMinimumRentalDays(carId, minimumRentalDays);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarAvailability> findByCarId(final long carId) {
        return carAvailabilityDao.findByCarId(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarAvailability> findEffectiveOfferedByCarPaginated(
            final long carId, final int page, final int pageSize) {
        return slicePage(effectiveOfferedAvailabilityFor(carId), page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookableSegmentProjection> getEffectiveSegmentsForOwnerCalendarInRangePaginated(
            final long carId,
            final LocalDate from,
            final LocalDate to,
            final int page,
            final int pageSize) {
        final List<BookableSegmentProjection> segments =
                carAvailabilityCalendarService.getEffectiveSegmentsForOwnerCalendarInRange(carId, from, to);
        return slicePage(segments, page, pageSize);
    }

    /**
     * In-memory page slice for effective/calendar projections.
     * Month-scoped lists are already bounded (typically tens of rows after merge).
     * Pushing {@code computeEffectiveOffered} into SQL is intentionally deferred:
     * effective rows are a JVM merge of overlapping OFFERED/WITHDRAWN intervals,
     * not a simple OFFSET query.
     */
    private static <T> Page<T> slicePage(final List<T> all, final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        final int total = all.size();
        final int fromIndex = Math.min(safePage * safePageSize, total);
        final int toIndex = Math.min(fromIndex + safePageSize, total);
        final List<T> content = fromIndex < toIndex ? all.subList(fromIndex, toIndex) : List.of();
        return new Page<>(content, safePage, safePageSize, total);
    }

    /**
     * Private helper that resolves the currently-effective OFFERED rows for the car via the
     * calendar service. Kept separate from the paginated public method so the resolution logic
     * has a single call site.
     */
    private List<CarAvailability> effectiveOfferedAvailabilityFor(final long carId) {
        return carAvailabilityCalendarService.findEffectiveOfferedByCar(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarAvailability> findByCarIdsEndingOnOrAfter(
            final Collection<Long> carIds,
            final LocalDate minEndDate) {
        return carAvailabilityDao.findByCarIdsEndingOnOrAfter(carIds, minEndDate);
    }

    private CarAvailability applyOwnerEditByCar(
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
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_INVALID_ORDER);
        }
        // Defense-in-depth: an edit can extend a window or move it forward, which both re-introduce
        // bookable days. applyOwnerWithdrawByCar is intentionally left unguarded — it only ever
        // *reduces* bookability and is the safety valve that a blocked owner may need to use.
        requireOwnerOfCarNotBlocked(carId);

        final List<DateRange> removed = CarAvailabilityCalendarMath.subtractDayRange(
                oldStartInclusive, oldEndInclusive, newStartInclusive, newEndInclusive);

        // Serialize against concurrent booking: acquire the per-car reservation-write lock before the
        // overlap check so a booking cannot slip in between the check and the OFFERED/WITHDRAWN writes.
        carService.lockForReservationWrite(carId);
        rejectIfReservationsOverlapAnyChunkByCar(carId, removed, MessageKeys.CAR_AVAILABILITY_EDIT_CONFLICT);

        final CarAvailability offered = carAvailabilityDao.createFullForCar(
                carId, newStartInclusive, newEndInclusive, dayPrice,
                startPointStreet, startPointNumber, neighborhoodId,
                checkInTime, checkOutTime, CarAvailability.Kind.OFFERED);

        for (final DateRange chunk : removed) {
            carAvailabilityDao.createFullForCar(
                    carId, chunk.start(), chunk.end(), dayPrice,
                    startPointStreet, startPointNumber, neighborhoodId,
                    checkInTime, checkOutTime, CarAvailability.Kind.WITHDRAWN);
        }

        return offered;
    }

    @Override
    @Transactional
    public CarAvailability applyOwnerWithdrawByCar(final long carId, final long availabilityId) {
        final CarAvailability target = carAvailabilityDao.findById(availabilityId)
                .orElseThrow(() -> new CarValidationException(MessageKeys.CAR_AVAILABILITY_NOT_FOUND));
        if (target.getCarId() != carId) {
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_NOT_OWNED);
        }
        if (target.getKind() != CarAvailability.Kind.OFFERED) {
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_NOT_OFFERED);
        }

        // Serialize against concurrent booking before the overlap check + WITHDRAWN write.
        carService.lockForReservationWrite(carId);
        final List<DateRange> withdrawnChunks = List.of(
                new DateRange(target.getStartInclusive(), target.getEndInclusive()));
        rejectIfReservationsOverlapAnyChunkByCar(carId, withdrawnChunks,
                MessageKeys.CAR_AVAILABILITY_WITHDRAW_CONFLICT);

        return carAvailabilityDao.createFullForCar(
                carId,
                target.getStartInclusive(),
                target.getEndInclusive(),
                target.getDayPriceValue(),
                target.getStartPointStreet(),
                target.getStartPointNumber().orElse(null),
                target.getNeighborhoodId().orElse(null),
                target.getCheckInTime(),
                target.getCheckOutTime(),
                CarAvailability.Kind.WITHDRAWN);
    }

    @Override
    @Transactional
    public void applyOwnerWithdrawRangeByCar(
            final long carId,
            final LocalDate startInclusive,
            final LocalDate endInclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endInclusive, "endInclusive");
        if (endInclusive.isBefore(startInclusive)) {
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_INVALID_ORDER);
        }
        requireOwnerOfCarNotBlocked(carId);

        // Serialize against concurrent booking before the overlap check + WITHDRAWN write.
        carService.lockForReservationWrite(carId);
        final List<DateRange> withdrawnChunks = List.of(new DateRange(startInclusive, endInclusive));
        rejectIfReservationsOverlapAnyChunkByCar(carId, withdrawnChunks,
                MessageKeys.CAR_AVAILABILITY_WITHDRAW_CONFLICT);

        final Optional<CarAvailability> template = findEffectiveForDayByCar(carId, startInclusive);
        final BigDecimal dayPrice = template.map(CarAvailability::getDayPriceValue).orElse(BigDecimal.ZERO);
        final String street = template.map(CarAvailability::getStartPointStreet).orElse("");
        final String number = template.flatMap(CarAvailability::getStartPointNumber).orElse(null);
        final Long neighborhoodId = template.flatMap(CarAvailability::getNeighborhoodId).orElse(null);
        final LocalTime checkIn = template.map(CarAvailability::getCheckInTime).orElse(LocalTime.of(10, 0));
        final LocalTime checkOut = template.map(CarAvailability::getCheckOutTime).orElse(LocalTime.of(20, 0));

        carAvailabilityDao.createFullForCar(
                carId,
                startInclusive,
                endInclusive,
                dayPrice,
                street,
                number,
                neighborhoodId,
                checkIn,
                checkOut,
                CarAvailability.Kind.WITHDRAWN);
    }

    @Override
    @Transactional
    public void parseAndApplyWithdrawRange(
            final long carId,
            final String fromInclusive,
            final String untilInclusive) {
        if (fromInclusive == null || fromInclusive.isBlank()
                || untilInclusive == null || untilInclusive.isBlank()) {
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_INVALID_ORDER);
        }
        final LocalDate start = LocalDate.parse(fromInclusive);
        final LocalDate end = LocalDate.parse(untilInclusive);
        applyOwnerWithdrawRangeByCar(carId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<AvailabilityPeriod>> getBookableWallAvailabilityPeriodsByCars(
            final Collection<Long> carIds) {
        return carAvailabilityCalendarService.getBookableWallAvailabilityPeriodsByCars(carIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Boolean> hasRiderBookableSegmentsByCarIds(
            final Collection<Long> carIds, final Instant now) {
        return carAvailabilityCalendarService.hasRiderBookableSegmentsByCarIds(carIds, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookableSegmentProjection> getBookableSegmentsForRiderDatePickerByCar(
            final long carId, final Instant now) {
        return carAvailabilityCalendarService.getBookableSegmentsForRiderDatePickerByCar(carId, now);
    }

    private void rejectIfReservationsOverlapAnyChunkByCar(
            final long carId, final List<DateRange> chunks, final String conflictMessageKey) {
        if (chunks.isEmpty()) {
            return;
        }
        final ZoneId wall = AppTimezone.WALL_ZONE;
        // Compute the [min start, max end] envelope so the helper does not rely on the caller
        // passing chunks pre-sorted by start.
        LocalDate envStart = chunks.get(0).start();
        LocalDate envEnd = chunks.get(0).end();
        for (final DateRange chunk : chunks) {
            if (chunk.start().isBefore(envStart)) {
                envStart = chunk.start();
            }
            if (chunk.end().isAfter(envEnd)) {
                envEnd = chunk.end();
            }
        }
        final OffsetDateTime fromUtc = envStart.atStartOfDay(wall).toOffsetDateTime();
        final OffsetDateTime toUtc = envEnd.plusDays(1).atStartOfDay(wall).toOffsetDateTime();
        final List<BlockingReservationProjection> blocking =
                reservationService.findBlockingReservationsByCarIdInRange(carId, fromUtc, toUtc);
        for (final BlockingReservationProjection r : blocking) {
            final LocalDate rStart = r.getStartDate().atZoneSameInstant(wall).toLocalDate();
            final LocalDate rEnd = r.getEndDate().atZoneSameInstant(wall).toLocalDate();
            for (final DateRange chunk : chunks) {
                if (!rEnd.isBefore(chunk.start()) && !rStart.isAfter(chunk.end())) {
                    throw new ReservationConflictException(conflictMessageKey);
                }
            }
        }
    }

    private static List<DateRange> periodsToDateRanges(final List<AvailabilityPeriod> periods) {
        if (periods == null || periods.isEmpty()) {
            return List.of();
        }
        final List<DateRange> out = new ArrayList<>(periods.size());
        for (final AvailabilityPeriod p : periods) {
            out.add(new DateRange(p.getStartInclusive(), p.getEndInclusive()));
        }
        return out;
    }

    private void validatePublicationAvailabilityRiderLead(
            final List<AvailabilityPeriod> periods, final LocalTime checkInTime, final Instant now) {
        if (periods == null || periods.isEmpty()) {
            return;
        }
        final int pickupLeadHours = reservationTimingPolicy.getPickupLeadHours();
        final LocalTime pickup = checkInTime != null ? checkInTime : CarAvailability.DEFAULT_CHECK_IN_TIME;
        final LocalDate minStart = RiderPickupLeadTime.minCarAvailabilityFirstDayInclusive(
                pickup, AppTimezone.WALL_ZONE, now, pickupLeadHours);
        for (int i = 0; i < periods.size(); i++) {
            final LocalDate from = periods.get(i).getStartInclusive();
            if (from.isBefore(minStart)) {
                throw new AvailabilityRiderLeadViolationException(
                        i, "validation.availabilityRow.from.riderLeadTime", minStart, pickupLeadHours);
            }
        }
    }

    private void validateEditAvailabilityRiderLead(
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

    private void validatePublicationAvailabilityAgainstWallCalendar(final List<AvailabilityPeriod> periods) {
        carAvailabilityPolicy.validateAvailabilityWithinPublishHorizon(
                LocalDate.now(AppTimezone.WALL_ZONE), periods);
    }

    @Override
    @Transactional
    public List<CarAvailability> createListing(
            final long ownerUserId, final long carId, final AvailabilityCreateInput input, final Instant now) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(now, "now");
        requireOwnerOfCarNotBlocked(carId);
        if (carService.isModelPendingValidation(carId)) {
            throw new CarValidationException(MessageKeys.CAR_CREATE_MODEL_PENDING);
        }
        final User owner = userService.getUserById(ownerUserId)
                .orElseThrow(() -> new CarValidationException(MessageKeys.CAR_NOT_FOUND));
        if (!userReadinessService.hasValidCbu(owner)) {
            throw new CarValidationException(MessageKeys.CAR_PUBLISH_CBU_REQUIRED);
        }
        validatePublicationAvailabilityRiderLead(input.periods(), input.checkInTime(), now);
        validatePublicationAvailabilityAgainstWallCalendar(input.periods());
        // Days that already hold active reservations cannot be re-published: the new OFFERED row
        // would be invisible to those reservations (bridge-anchored) and to any new booking
        // (hasActiveOverlapByCar blocks them), so the publication is product-meaningless. Rejecting
        // here keeps the picker's disabled-days contract honest server-side.
        // Serialize against concurrent booking before the overlap check + OFFERED writes.
        carService.lockForReservationWrite(carId);
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new CarValidationException(MessageKeys.CAR_NOT_FOUND));
        if (car.getOwnerId() != ownerUserId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        // Publishing availability is only meaningful for listings that can return to the market
        // (ACTIVE or PAUSED). DEACTIVATED / LACK_DOC / ADMIN_PAUSED must be restored first.
        // Bookable-segments and POST /reservations still require ACTIVE.
        if (car.getStatus() != Car.Status.ACTIVE && car.getStatus() != Car.Status.PAUSED) {
            throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
        rejectIfReservationsOverlapAnyChunkByCar(
                carId,
                periodsToDateRanges(input.periods()),
                MessageKeys.CAR_AVAILABILITY_PUBLISH_CONFLICT);
        return createCarAvailabilityPeriods(
                carId,
                input.pricePerDay(),
                input.startPointStreet(),
                input.rawStartPointNumber(),
                input.rawNeighborhoodId(),
                input.checkInTime(),
                input.checkOutTime(),
                input.periods(),
                input.periodPrices(),
                input.minimumRentalDays());
    }

    @Override
    @Transactional
    public CarAvailability editAvailability(
            final long ownerUserId,
            final long carId,
            final long availabilityId,
            final AvailabilityCreateInput input,
            final Instant now) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(now, "now");
        final CarAvailability old = carAvailabilityDao.findById(availabilityId)
                .orElseThrow(() -> new CarValidationException(MessageKeys.CAR_AVAILABILITY_NOT_FOUND));
        if (old.getCarId() != carId) {
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_NOT_OWNED);
        }
        validateEditAvailabilityRiderLead(
                input.periods(), input.checkInTime(), now, old.getStartInclusive());
        validatePublicationAvailabilityAgainstWallCalendar(input.periods());
        if (input.periods().isEmpty()) {
            throw new CarValidationException(MessageKeys.CAR_AVAILABILITY_REQUIRED);
        }
        final AvailabilityPeriod newPeriod = input.periods().get(0);
        return applyOwnerEditByCar(
                carId,
                old.getStartInclusive(), old.getEndInclusive(),
                newPeriod.getStartInclusive(), newPeriod.getEndInclusive(),
                input.pricePerDay(),
                input.startPointStreet(), input.rawStartPointNumber(),
                input.rawNeighborhoodId(),
                input.checkInTime(), input.checkOutTime());
    }

    /**
     * Throws {@link CarValidationException} with {@link MessageKeys#CAR_MUTATION_OWNER_BLOCKED} when the
     * owner of {@code carId} is currently blocked. Used by mutations that could re-introduce a bookable car
     * (create / edit availability). Reads the car through {@link CarService} and the owner through
     * {@link UserService} to respect the "service may only call its own DAO" rule.
     */
    private void requireOwnerOfCarNotBlocked(final long carId) {
        final long ownerId = carService.getCarById(carId)
                .map(c -> c.getOwnerId())
                .orElseThrow(() -> new CarValidationException(MessageKeys.CAR_NOT_FOUND));
        if (userService.getUserById(ownerId).map(u -> u.isBlocked()).orElse(false)) {
            throw new CarValidationException(MessageKeys.CAR_MUTATION_OWNER_BLOCKED);
        }
    }
}
