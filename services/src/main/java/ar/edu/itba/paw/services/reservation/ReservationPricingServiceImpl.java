package ar.edu.itba.paw.services.reservation;


import static ar.edu.itba.paw.util.ReservationServiceSupport.isBlank;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.models.util.time.BillableDays;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.util.format.MoneyFormat;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarService;
/**
 * Pricing, day math, and rider-side validation guards. Shared by submit and edit flows in
 * {@link ReservationWorkflowServiceImpl} so the same calendar / billable-day math is applied
 * in both entry points.
 */
@Service
public class ReservationPricingServiceImpl implements ReservationPricingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationPricingServiceImpl.class);

    private final CarAvailabilityService carAvailabilityService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final MoneyFormat moneyFormat;
    private final CarService carService;
    private final ReservationService reservationService;

    @Autowired
    public ReservationPricingServiceImpl(
            final CarAvailabilityService carAvailabilityService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final MoneyFormat moneyFormat,
            @Lazy final CarService carService,
            @Lazy final ReservationService reservationService) {
        this.carAvailabilityService = carAvailabilityService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.moneyFormat = moneyFormat;
        this.carService = carService;
        this.reservationService = reservationService;
    }

    // ---------------------------------------------------------------------------------------
    // Timing policy
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredPickupLeadHours() {
        return reservationTimingPolicy.getPickupLeadHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredPaymentProofDeadlineHours() {
        return reservationTimingPolicy.getPaymentProofDeadlineHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredRefundProofDeadlineHours() {
        return reservationTimingPolicy.getRefundProofDeadlineHours();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredReturnReminderHoursBeforeCheckout() {
        return reservationTimingPolicy.getReturnReminderHoursBeforeCheckout();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredMaxReservationBillableDays() {
        return reservationTimingPolicy.getMaxBillableDaysPerReservation();
    }

    // ---------------------------------------------------------------------------------------
    // Pricing / day math
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Optional<String> normalizeClientReservationTotal(final String reservationTotal) {
        if (isBlank(reservationTotal)) {
            return Optional.empty();
        }
        final String trimmed = reservationTotal.trim();
        if (!trimmed.matches("\\d+(?:\\.\\d+)?")) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> reservationTotalDisplayByCar(
            final Long carId, final String fromDateTime, final String untilDateTime) {
        return reservationTotalDisplayByCar(carId, null, fromDateTime, untilDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> reservationTotalDisplayByCar(
            final Long carId,
            final Long availabilityId,
            final String fromDateTime,
            final String untilDateTime) {
        if (carId == null || isBlank(fromDateTime) || isBlank(untilDateTime)) {
            return Optional.empty();
        }
        final var car = carService.getCarById(carId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        final OffsetDateTime[] interval = parseRiderReservationInterval(fromDateTime, untilDateTime);
        final OffsetDateTime startDate = interval[0];
        final OffsetDateTime endDate = interval[1];
        validateRiderReservationPricingInterval(
                carId, availabilityId, startDate, endDate, car.getMinimumRentalDays());
        if (reservationService.hasActiveOverlapByCar(carId, startDate, endDate)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        return calculateTotalByCar(carId, startDate, endDate)
                .filter(total -> total.signum() > 0)
                .map(moneyFormat::format);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateRiderReservationPricingInterval(
            final long carId,
            final Long availabilityId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final int minimumRentalDays) {
        validateWallPickupDateNotBeforeToday(startDate);
        validatePickupAtLeastConfiguredLeadAhead(startDate);
        if (!reservationIntervalFitsCarAvailability(carId, availabilityId, startDate, endDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY);
        }
        validateHandoverTimesMatchEffectiveAvailability(carId, startDate, endDate);
        final long billableDays = calculateBillableDays(startDate, endDate);
        final int maxBillableDays = getConfiguredMaxReservationBillableDays();
        if (billableDays > maxBillableDays) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_MAX_BILLABLE_DAYS, maxBillableDays);
        }
        if (billableDays < minimumRentalDays) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_BELOW_MINIMUM_DAYS, minimumRentalDays);
        }
    }

    private static OffsetDateTime[] parseRiderReservationInterval(
            final String fromDateTime, final String untilDateTime) {
        if (isBlank(fromDateTime) || isBlank(untilDateTime)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_REQUIRED);
        }
        final OffsetDateTime startDate;
        final OffsetDateTime endDate;
        try {
            startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(fromDateTime);
            endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(untilDateTime);
        } catch (final DateTimeParseException e) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_INVALID_FORMAT);
        }
        if (!endDate.isAfter(startDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_END_NOT_AFTER_START);
        }
        return new OffsetDateTime[] {startDate, endDate};
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calculateTotalByCar(
            final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        if (calculateBillableDays(startDate, endDate) <= 0) {
            return Optional.empty();
        }
        final LocalDate firstBillableDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate lastBillableDay = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        return planReservationByCar(carId, firstBillableDay, lastBillableDay).map(ReservationPlan::total);
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateBillableDays(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        return BillableDays.between(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationPlan> planReservationByCar(
            final long carId, final LocalDate firstBillableDay, final LocalDate lastBillableDay) {
        // One overlapping-range query; effective row per day is the first cover in
        // (createdAt DESC, id DESC) order — same precedence as findEffectiveForDayByCar.
        final List<CarAvailability> overlapping =
                carAvailabilityService.findOverlappingRangeByCar(carId, firstBillableDay, lastBillableDay);
        BigDecimal total = BigDecimal.ZERO;
        final LinkedHashSet<Long> coveringIds = new LinkedHashSet<>();
        CarAvailability firstDayAvailability = null;
        for (LocalDate day = firstBillableDay; !day.isAfter(lastBillableDay); day = day.plusDays(1)) {
            final Optional<CarAvailability> effective = resolveEffectiveForDay(overlapping, day);
            if (effective.isEmpty() || effective.get().getKind() == CarAvailability.Kind.WITHDRAWN) {
                return Optional.empty();
            }
            final CarAvailability av = effective.get();
            if (firstDayAvailability == null) {
                firstDayAvailability = av;
            }
            coveringIds.add(av.getId());
            total = total.add(av.getDayPriceValue());
        }
        if (firstDayAvailability == null) {
            return Optional.empty();
        }
        return Optional.of(new ReservationPlan(total, coveringIds, firstDayAvailability));
    }

    // ---------------------------------------------------------------------------------------
    // Rider input validations
    // ---------------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public void validateWallPickupDateNotBeforeToday(final OffsetDateTime startDate) {
        final LocalDate pickupDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);
        if (pickupDay.isBefore(today)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_NOT_FROM_TODAY);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePickupAtLeastConfiguredLeadAhead(final OffsetDateTime startDate) {
        final Instant now = Instant.now();
        final int pickupLeadHours = reservationTimingPolicy.getPickupLeadHours();
        if (!startDate.toInstant().isAfter(now.plus(pickupLeadHours, ChronoUnit.HOURS))) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_PICKUP_MIN_24H, pickupLeadHours);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateHandoverTimesMatchEffectiveAvailability(
            final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        final LocalDate pickupDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate returnDay = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalTime submittedCheckIn = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalTime();
        final LocalTime submittedCheckOut = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalTime();

        final CarAvailability pickupAv = carAvailabilityService
                .findEffectiveForDayByCar(carId, pickupDay)
                .filter(a -> a.getKind() == CarAvailability.Kind.OFFERED)
                .orElseThrow(() -> new RiderReservationException(
                        MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY));
        if (!submittedCheckIn.equals(pickupAv.getCheckInTime())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_HANDOVER_TIME_MISMATCH);
        }

        final CarAvailability returnAv = pickupDay.equals(returnDay)
                ? pickupAv
                : carAvailabilityService
                        .findEffectiveForDayByCar(carId, returnDay)
                        .filter(a -> a.getKind() == CarAvailability.Kind.OFFERED)
                        .orElseThrow(() -> new RiderReservationException(
                                MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY));
        if (!submittedCheckOut.equals(returnAv.getCheckOutTime())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_HANDOVER_TIME_MISMATCH);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean reservationIntervalFitsCarAvailability(
            final long carId,
            final Long availabilityId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final LocalDate firstDay = startDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        final LocalDate lastDay = endDate.atZoneSameInstant(AppTimezone.WALL_ZONE).toLocalDate();
        if (availabilityId != null) {
            final Optional<CarAvailability> specific = carAvailabilityService.findById(availabilityId);
            if (specific.isEmpty() || specific.get().getKind() != CarAvailability.Kind.OFFERED) {
                return false;
            }
            final CarAvailability av = specific.get();
            if (firstDay.isBefore(av.getStartInclusive()) || lastDay.isAfter(av.getEndInclusive())) {
                return false;
            }
        }
        final List<CarAvailability> overlapping =
                carAvailabilityService.findOverlappingRangeByCar(carId, firstDay, lastDay);
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            final Optional<CarAvailability> eff = resolveEffectiveForDay(overlapping, day);
            if (eff.isEmpty() || eff.get().getKind() == CarAvailability.Kind.WITHDRAWN) {
                return false;
            }
        }
        return true;
    }

    /**
     * First overlapping row that covers {@code day}. {@code overlapping} must already be ordered by
     * {@code createdAt DESC, id DESC} (see {@code CarAvailabilityDao#findOverlappingRangeByCar}).
     */
    private static Optional<CarAvailability> resolveEffectiveForDay(
            final List<CarAvailability> overlapping, final LocalDate day) {
        return overlapping.stream()
                .filter(a -> !a.getStartInclusive().isAfter(day) && !a.getEndInclusive().isBefore(day))
                .findFirst();
    }
}
