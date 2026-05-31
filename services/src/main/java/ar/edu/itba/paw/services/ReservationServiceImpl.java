package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.exception.user.CBUNotFoundException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.ReservationHubStatusWhitelist;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.OwnerRefundProofObligationEmailPayload;
import ar.edu.itba.paw.models.email.RiderRefundProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;


/**
 * Reservation workflows: persists reservations through {@link ReservationDao}; pricing breakdown rows through
 * {@link ReservationAvailabilityService}. Listings, users, stored files, and mail go through peer services.
 */
@Service
public final class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationDao reservationDao;
    private final ReservationAvailabilityService reservationAvailabilityService;
    private final ListingAvailabilityService listingAvailabilityService;
    private final ListingAddressFormatter listingAddressFormatter;
    private final UserService userService;
    private final EmailService emailService;
    private final StoredFileService storedFileService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final PaginationPolicy paginationPolicy;
    private final CarService carService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ReservationAvailabilityService reservationAvailabilityService,
            final ListingAvailabilityService listingAvailabilityService,
            final ListingAddressFormatter listingAddressFormatter,
            final UserService userService,
            final EmailService emailService,
            final StoredFileService storedFileService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            final PaginationPolicy paginationPolicy,
            final CarService carService) {
        this.reservationDao = reservationDao;
        this.reservationAvailabilityService = reservationAvailabilityService;
        this.listingAvailabilityService = listingAvailabilityService;
        this.listingAddressFormatter = listingAddressFormatter;
        this.userService = userService;
        this.emailService = emailService;
        this.storedFileService = storedFileService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.paymentReceiptUploadPolicy = paymentReceiptUploadPolicy;
        this.paginationPolicy = paginationPolicy;
        this.carService = carService;
    }

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
    public int getConfiguredReturnReminderHoursBeforeCheckout() {
        return reservationTimingPolicy.getReturnReminderHoursBeforeCheckout();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredMaxReservationBillableDays() {
        return reservationTimingPolicy.getMaxBillableDaysPerReservation();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservationById(final long id) {
        return reservationDao.getReservationById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getRiderReservationById(final long riderId, final long reservationId) {
        return reservationDao.getReservationById(reservationId)
                .filter(reservation -> reservation.getRiderId() == riderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getOwnerReservationById(final long ownerId, final long reservationId) {
        return reservationDao.getOwnerReservationById(ownerId, reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getRiderReservationCards(final ReservationSearchCriteria criteria) {
        return reservationDao.getRiderReservationCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getOwnerReservationCards(final ReservationSearchCriteria criteria) {
        return reservationDao.getOwnerReservationCards(criteria);
    }

    private static final Set<String> RATING_BANDS = Set.of("UNDER_2", "2_TO_3", "3_TO_4", "OVER_4");

    @Override
    @Transactional(readOnly = true)
    public ReservationSearchCriteria buildReservationSearchCriteria(
            final Long ownerId,
            final Long riderId,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final List<String> statusFilter,
            final int page,
            final String sort,
            final String textQuery,
            final Long carId) {
        final List<String> carTypes = collectCarTypeParams(category);
        final List<String> transmissions = collectTransmissionParams(transmission);
        final List<String> powertrains = collectPowertrainParams(powertrain);
        final BigDecimal minPrice = priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0 ? priceMin : null;
        final BigDecimal maxPrice = priceMax != null && priceMax.compareTo(BigDecimal.ZERO) >= 0 ? priceMax : null;
        final ArrayList<String> statuses = new ArrayList<>();
        if (statusFilter != null) {
            for (final String s : statusFilter) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                final String low = s.trim().toLowerCase();
                if (ReservationHubStatusWhitelist.contains(low)) {
                    statuses.add(low);
                }
            }
        }
        final ArrayList<String> ratingBands = new ArrayList<>();
        if (rating != null) {
            for (final String r : rating) {
                if (r == null || r.isBlank()) {
                    continue;
                }
                final String u = r.trim().toUpperCase();
                if (RATING_BANDS.contains(u)) {
                    ratingBands.add(u);
                }
            }
        }
        final String[] sortParts = (sort != null && !sort.isBlank()) ? sort.split(",", 2) : new String[0];
        final String sortBy = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        return new ReservationSearchCriteria(
                ownerId, riderId, carId, page, paginationPolicy.getDefaultPageSize(), statuses,
                carTypes, transmissions, powertrains, minPrice, maxPrice, ratingBands, sortBy, sortDir, textQuery);
    }

    private static List<String> collectCarTypeParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Type.valueOf(u);
                if (!out.contains(u)) {
                    out.add(u);
                }
            } catch (final IllegalArgumentException ex) {
                LOGGER.atDebug()
                        .setMessage("Ignoring invalid car type reservation hub filter token [{}]")
                        .addArgument(u)
                        .setCause(ex)
                        .log();
            }
        }
        return out;
    }

    private static List<String> collectTransmissionParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Transmission.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ex) {
                LOGGER.atDebug()
                        .setMessage("Ignoring invalid transmission reservation hub filter token [{}]")
                        .addArgument(u)
                        .setCause(ex)
                        .log();
            }
        }
        return out;
    }

    private static List<String> collectPowertrainParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Powertrain.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ex) {
                LOGGER.atDebug()
                        .setMessage("Ignoring invalid powertrain reservation hub filter token [{}]")
                        .addArgument(u)
                        .setCause(ex)
                        .log();
            }
        }
        return out;
    }

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
            final Long carId,
            final String fromDateTime,
            final String untilDateTime) {
        if (carId == null || isBlank(fromDateTime) || isBlank(untilDateTime)) {
            return Optional.empty();
        }
        try {
            final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(fromDateTime);
            final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(untilDateTime);
            return calculateTotalByCar(carId, startDate, endDate).map(ArsMoneyFormat::format);
        } catch (final DateTimeParseException e) {
            LOGGER.atDebug()
                    .setMessage("reservationTotalDisplayByCar: unparseable wall datetimes carId={} from=[{}] until=[{}]")
                    .addArgument(carId)
                    .addArgument(fromDateTime)
                    .addArgument(untilDateTime)
                    .setCause(e)
                    .log();
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calculateTotalByCar(
            final long carId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        if (calculateBillableDays(startDate, endDate) <= 0) {
            return Optional.empty();
        }
        final LocalDate firstBillableDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate lastBillableDay = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        return planReservationByCar(carId, firstBillableDay, lastBillableDay).map(ReservationPlan::total);
    }

    @Override
    @Transactional
    public Reservation submitRiderReservationByCar(
            final long riderId,
            final long carId,
            final Long availabilityId,
            final String fromDateTime,
            final String untilDateTime) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        // Defense in depth: the catalog already hides blocked-owner cars, but a direct POST should also fail.
        if (car.getOwner() != null && car.getOwner().isBlocked()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_OWNER_BLOCKED);
        }
        final User rider = userService.getUserById(riderId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_USER_NOT_FOUND));
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
        validateWallPickupDateNotBeforeToday(startDate);
        validatePickupAtLeastTwentyFourHoursInAdvance(startDate);
        if (!reservationIntervalFitsCarAvailability(carId, availabilityId, startDate, endDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY);
        }
        validateHandoverTimesMatchEffectiveAvailability(carId, startDate, endDate);
        if (car.getOwner().getId() == rider.getId()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING);
        }
        if (!userService.hasUploadedLicenseAndIdentity(rider)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DOCUMENTATION_REQUIRED);
        }
        final String cbu;
        try {
            cbu = userService.getUserCbu(car.getOwner().getId());
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atWarn().setCause(e).addArgument(car.getOwner().getId()).log("Owner payment details unavailable for ownerId={}");
            throw new RiderReservationException(MessageKeys.RESERVATION_OWNER_PAYMENT_DETAILS_UNAVAILABLE);
        }
        final OffsetDateTime proofDeadline = OffsetDateTime.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPaymentProofDeadlineHours(), ChronoUnit.HOURS),
                ZoneOffset.UTC);
        return createReservationForCar(
                rider.getId(),
                carId,
                startDate,
                endDate,
                Reservation.Status.PENDING,
                proofDeadline,
                cbu);
    }

    private boolean reservationIntervalFitsCarAvailability(
            final long carId,
            final Long availabilityId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final LocalDate firstDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate lastDay = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        if (availabilityId != null) {
            final Optional<ListingAvailability> specific = listingAvailabilityService.findById(availabilityId);
            if (specific.isEmpty() || specific.get().getKind() != ListingAvailability.Kind.OFFERED) {
                return false;
            }
            final ListingAvailability av = specific.get();
            return !firstDay.isBefore(av.getStartInclusive()) && !lastDay.isAfter(av.getEndInclusive());
        }
        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            final Optional<ListingAvailability> eff = listingAvailabilityService.findEffectiveForDayByCar(carId, day);
            if (eff.isEmpty() || eff.get().getKind() == ListingAvailability.Kind.WITHDRAWN) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    private Reservation createReservationForCar(
            final long riderId,
            final long carId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final OffsetDateTime paymentProofDeadlineAt,
            final String ownerCbu) {
        final long billableDays = calculateBillableDays(startDate, endDate);
        if (billableDays > reservationTimingPolicy.getMaxBillableDaysPerReservation()) {
            throw new RiderReservationException(
                    MessageKeys.RESERVATION_RIDER_MAX_BILLABLE_DAYS,
                    reservationTimingPolicy.getMaxBillableDaysPerReservation());
        }
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        if (billableDays < car.getMinimumRentalDays()) {
            throw new RiderReservationException(
                    MessageKeys.RESERVATION_RIDER_BELOW_MINIMUM_DAYS,
                    car.getMinimumRentalDays());
        }
        if (reservationDao.hasActiveOverlapByCar(carId, startDate, endDate)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final LocalDate firstBillableDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate lastBillableDay = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final ReservationPlan plan = planReservationByCar(carId, firstBillableDay, lastBillableDay)
                .filter(p -> p.total().signum() > 0)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_TOTAL_PRICE_INVALID));
        final Reservation reservation =
                reservationDao.createReservationForCar(
                        riderId, carId, startDate, endDate, status, plan.total(), paymentProofDeadlineAt);
        reservationAvailabilityService.insertCoveringAvailabilities(reservation.getId(), plan.coveringAvailabilityIds());
        enqueueReservationConfirmationEmailForCar(riderId, carId, reservation, plan.firstDayAvailability(), ownerCbu);
        LOGGER.atInfo()
                .addArgument(reservation.getId())
                .addArgument(carId)
                .addArgument(riderId)
                .addArgument(status)
                .log("Created car-based reservation id={} carId={} riderId={} status={}");
        return reservation;
    }

    /**
     * Day-by-day pricing plan for a reservation: for each wall-calendar day in
     * {@code [firstBillableDay, lastBillableDay]}, picks the {@code OFFERED} availability of the car
     * that wins by latest {@code createdAt} ({@link ListingAvailabilityService#findEffectiveForDayByCar}),
     * accumulating the running total and the distinct set of winning availability ids in their first
     * appearance order. Returns empty when any day lacks an effective offered availability.
     */
    private Optional<ReservationPlan> planReservationByCar(
            final long carId,
            final LocalDate firstBillableDay,
            final LocalDate lastBillableDay) {
        BigDecimal total = BigDecimal.ZERO;
        final java.util.LinkedHashSet<Long> coveringIds = new java.util.LinkedHashSet<>();
        ListingAvailability firstDayAvailability = null;
        for (LocalDate day = firstBillableDay; !day.isAfter(lastBillableDay); day = day.plusDays(1)) {
            final Optional<ListingAvailability> effective =
                    listingAvailabilityService.findEffectiveForDayByCar(carId, day);
            if (effective.isEmpty() || effective.get().getKind() == ListingAvailability.Kind.WITHDRAWN) {
                return Optional.empty();
            }
            final ListingAvailability av = effective.get();
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

    private record ReservationPlan(
            BigDecimal total,
            java.util.LinkedHashSet<Long> coveringAvailabilityIds,
            ListingAvailability firstDayAvailability) {
    }

    private void enqueueReservationConfirmationEmailForCar(
            final long riderId,
            final long carId,
            final Reservation reservation,
            final ListingAvailability pickupAvailability,
            final String ownerCbu) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<Car> carOpt = carService.getCarById(carId);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(riderId).addArgument(reservation.getId()).log("Skipping car reservation confirmation email: rider not found for riderId={} reservationId={}");
                return;
            }
            if (carOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(carId).addArgument(reservation.getId()).log("Skipping car reservation confirmation email: car not found for carId={} reservationId={}");
                return;
            }
            final User rider = riderOpt.get();
            final Car car = carOpt.get();
            final User carOwner = car.getOwner();
            if (carOwner == null) {
                LOGGER.atWarn().addArgument(carId).addArgument(reservation.getId()).log("Skipping car reservation confirmation email: car owner not found for carId={} reservationId={}");
                return;
            }
            final String vehicleLabel = car.getBrand() + " " + car.getModel();
            final String handoverLocation = formatHandoverLocation(pickupAvailability);
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .reservationId(reservation.getId())
                    .carId(carId)
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(handoverLocation)
                    .ownerHandoverLocation(handoverLocation)
                    .ownerFullName(carOwner.getForename() + " " + carOwner.getSurname())
                    .ownerEmail(carOwner.getEmail())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userService.resolveMailLocale(rider.getId()))
                    .ownerMailLocale(userService.resolveMailLocale(carOwner.getId()))
                    .ownerCbu(ownerCbu)
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing car-based reservation confirmation email to {} for reservation id={}");
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId()).log("Could not enqueue car-based reservation confirmation email for reservation id={}");
        }
    }

    private static String formatHandoverLocation(final ListingAvailability availability) {
        if (availability == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(availability.getStartPointStreet());
        availability.getStartPointNumber().ifPresent(n -> sb.append(" ").append(n));
        return sb.toString().trim();
    }

    /**
     * Resolves the car owner from a reservation by reading the {@code Car} entity directly.
     */
    private Optional<User> resolveOwnerFromReservation(final Reservation reservation) {
        return Optional.ofNullable(reservation.getCar()).map(Car::getOwner);
    }

    /**
     * Most-recent availability row for the reservation's car, used to format
     * pickup/return address lines in transactional emails.
     */
    private Optional<ListingAvailability> resolveAvailabilityForReservation(final Reservation reservation) {
        return Optional.ofNullable(reservation.getCar())
                .map(Car::getId)
                .flatMap(listingAvailabilityService::findMostRecentByCarId);
    }

    /**
     * Returns a human-readable vehicle label using the car's brand and model.
     * Falls back to an empty string when the reservation has no associated car.
     */
    private String resolveVehicleLabelFromReservation(final Reservation reservation) {
        return Optional.ofNullable(reservation.getCar())
                .map(c -> c.getBrand() + " " + c.getModel())
                .orElse("");
    }

    @Override
    @Transactional(readOnly = true)
    public long calculateBillableDays(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return 0;
        }
        final LocalDate pickupDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate returnDay = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        return Math.max(1L, ChronoUnit.DAYS.between(pickupDay, returnDay.plusDays(1)));
    }

    private static void validateWallPickupDateNotBeforeToday(final OffsetDateTime startDate) {
        final LocalDate pickupDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        if (pickupDay.isBefore(today)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_NOT_FROM_TODAY);
        }
    }

    private void validatePickupAtLeastTwentyFourHoursInAdvance(final OffsetDateTime startDate) {
        final Instant now = Instant.now();
        final int pickupLeadHours = reservationTimingPolicy.getPickupLeadHours();
        if (!startDate.toInstant().isAfter(now.plus(pickupLeadHours, ChronoUnit.HOURS))) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_PICKUP_MIN_24H, pickupLeadHours);
        }
    }

    /**
     * Defensive check against a tampered client payload: the wall-time component of {@code startDate}
     * must equal the {@code checkInTime} of the effective {@link ListingAvailability} for the pickup day,
     * and the wall-time component of {@code endDate} must equal the {@code checkOutTime} of the effective
     * availability for the return day. The UI fixes these times from the bookable segment shown to the
     * rider, so any mismatch indicates the rider submitted hand-edited values or the effective
     * availability changed between picking dates and submitting.
     *
     * <p>Callers must have already validated that every day in the range is covered by an
     * {@link ar.edu.itba.paw.models.domain.ListingAvailability.Kind#OFFERED} availability (see
     * {@code reservationIntervalFitsCarAvailability}). This method assumes that precondition and only
     * checks the two boundary times.
     */
    void validateHandoverTimesMatchEffectiveAvailability(
            final long carId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
        final LocalDate pickupDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate returnDay = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalTime submittedCheckIn = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalTime();
        final LocalTime submittedCheckOut = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalTime();

        final ListingAvailability pickupAv = listingAvailabilityService
                .findEffectiveForDayByCar(carId, pickupDay)
                .filter(a -> a.getKind() == ListingAvailability.Kind.OFFERED)
                .orElseThrow(() -> new RiderReservationException(
                        MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY));
        if (!submittedCheckIn.equals(pickupAv.getCheckInTime())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_HANDOVER_TIME_MISMATCH);
        }

        final ListingAvailability returnAv = pickupDay.equals(returnDay)
                ? pickupAv
                : listingAvailabilityService
                        .findEffectiveForDayByCar(carId, returnDay)
                        .filter(a -> a.getKind() == ListingAvailability.Kind.OFFERED)
                        .orElseThrow(() -> new RiderReservationException(
                                MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY));
        if (!submittedCheckOut.equals(returnAv.getCheckOutTime())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_HANDOVER_TIME_MISMATCH);
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Optional<Reservation> performCancellationAndNotify(
            final long reservationId,
            final Reservation.Status cancelledStatus) {
        reservationDao.updateReservationStatus(reservationId, cancelledStatus.name().toLowerCase(Locale.ROOT));
        final Optional<Reservation> reservationOpt = reservationDao.getReservationById(reservationId);
        reservationOpt.ifPresent(r -> enqueueCancellationEmail(r, true));
        return reservationOpt;
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservation(final long reservationId) {
        final Optional<Reservation> cancelled =
                performCancellationAndNotify(reservationId, Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF);
        cancelled.ifPresent(
                ignored -> LOGGER.atInfo().addArgument(reservationId).log("Reservation id={} cancelled (missing payment proof)"));
        return cancelled;
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservationAsParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> opt = reservationDao.getReservationById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = opt.get();
        final boolean isRider = r.getRiderId() == userId;
        final boolean isOwner = Optional.ofNullable(r.getCar())
                .map(c -> c.getOwner().getId() == userId)
                .orElse(false);
        if (!isRider && !isOwner) {
            return Optional.empty();
        }
        final Reservation.Status cancelledStatus =
                isRider ? Reservation.Status.CANCELLED_BY_RIDER : Reservation.Status.CANCELLED_BY_OWNER;
        final OffsetDateTime nowUtc = OffsetDateTime.now(ZoneOffset.UTC);
        return switch (r.getStatus()) {
            case PENDING -> {
                if (r.getPaymentReceiptFileId().isPresent()) {
                    yield Optional.empty();
                }
                reservationDao.updateParticipantCancellationWithRefundMeta(
                        reservationId, cancelledStatus.name().toLowerCase(Locale.ROOT), false, null);
                final Optional<Reservation> cancelled = reservationDao.getReservationById(reservationId);
                cancelled.ifPresent(res -> {
                    enqueueCancellationEmail(res, true);
                    LOGGER.atInfo()
                            .addArgument(userId)
                            .addArgument(reservationId)
                            .addArgument(cancelledStatus)
                            .log("Participant userId={} cancelled reservation id={} ({})");
                });
                yield cancelled;
            }
            case ACCEPTED -> {
                if (!nowUtc.isBefore(r.getStartDate())) {
                    yield Optional.empty();
                }
                final boolean refundRequired = r.getPaymentReceiptFileId().isPresent();
                final OffsetDateTime refundDeadline = refundRequired
                        ? nowUtc.plusHours(reservationTimingPolicy.getPaymentProofDeadlineHours())
                        : null;
                reservationDao.updateParticipantCancellationWithRefundMeta(
                        reservationId,
                        cancelledStatus.name().toLowerCase(Locale.ROOT),
                        refundRequired,
                        refundDeadline);
                final Optional<Reservation> cancelled = reservationDao.getReservationById(reservationId);
                cancelled.ifPresent(res -> {
                    enqueueCancellationEmail(res, !refundRequired);
                    if (refundRequired) {
                        enqueueOwnerRefundProofObligationEmail(res, false);
                    }
                    LOGGER.atInfo()
                            .addArgument(userId)
                            .addArgument(reservationId)
                            .addArgument(cancelledStatus)
                            .log("Participant userId={} cancelled reservation id={} ({})");
                });
                yield cancelled;
            }
            default -> Optional.empty();
        };
    }

    private void enqueueCancellationEmail(final Reservation reservation, final boolean notifyOwnerCancellation) {
        try {
            final long riderId = reservation.getRiderId();
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<ListingAvailability> availabilityOpt = resolveAvailabilityForReservation(reservation);
            final Optional<User> listingOwnerOpt = resolveOwnerFromReservation(reservation);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(riderId).addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: user not found for riderId={} reservationId={}");
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: owner not found for reservationId={}");
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final String vehicleLabel = resolveVehicleLabelFromReservation(reservation);
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String riderLoc = availabilityOpt.map(a -> trimToNull(listingAddressFormatter.formatRiderReservationHandoverSummary(a, reservation))).orElse(null);
            final String ownerLoc = availabilityOpt.map(a -> trimToNull(listingAddressFormatter.formatOwnerReservationHandoverSummary(a))).orElse(null);
            final ReservationMailPayload mail = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(riderFullName)
                    .reservationId(reservation.getId())
                    .carId(reservation.getCarId())
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(riderLoc)
                    .ownerHandoverLocation(ownerLoc)
                    .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                    .ownerEmail(listingOwner.getEmail())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userService.resolveMailLocale(rider.getId()))
                    .ownerMailLocale(userService.resolveMailLocale(listingOwner.getId()))
                    .build();
            final ReservationCancellationEmailPayload cancelPayload = ReservationCancellationEmailPayload.builder()
                    .mail(mail)
                    .cancellationStatus(reservation.getStatus())
                    .notifyOwnerCancellation(notifyOwnerCancellation)
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing reservation cancellation email to {} for reservation id={}");
            emailService.sendReservationCancellationEmail(cancelPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue reservation cancellation email for reservation id={}");
        }
    }

    /**
     * Batch cancellation for expired proof deadlines. Uses {@link #performCancellationAndNotify} directly so all
     * iterations run in this method’s single {@code @Transactional} boundary (no self-invocation through
     * {@code cancelReservation}, which would ignore that method’s transaction metadata when called from here).
     */
    @Override
    @Transactional
    public void cancelExpiredPendingPaymentReservations() {
        final Reservation.Status status = Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF;
        int cancelled = 0;
        for (final Reservation r : reservationDao.findPendingPaymentPastDeadline(OffsetDateTime.now(ZoneOffset.UTC))) {
            performCancellationAndNotify(r.getId(), status);
            cancelled++;
        }
        LOGGER.atInfo().addArgument(cancelled).log("Payment proof deadline sweep: cancelled {} pending reservation(s)");
    }

    @Override
    @Transactional
    public void attachPaymentReceipt(
            final long riderId,
            final long reservationId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        if (!StoredFile.isAllowedPaymentReceiptContentType(contentType)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        final int len = data == null ? 0 : data.length;
        if (len == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        if (len > paymentReceiptUploadPolicy.getMaxBytes()) {
            throw new RiderReservationException(
                    MessageKeys.RESERVATION_PAYMENT_RECEIPT_TOO_LARGE, paymentReceiptUploadPolicy.getMaxMegabytesRoundedUp());
        }
        final Reservation r = getRiderReservationById(riderId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND));
        if (r.getStatus() != Reservation.Status.PENDING) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        final StoredFile file = storedFileService.create(riderId, originalFilename, contentType, data);
        final int updated = reservationDao.attachPaymentReceiptAndAccept(reservationId, riderId, file.getId());
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_RECEIPT_INVALID);
        }
        final Reservation afterAttach = getRiderReservationById(riderId, reservationId).orElse(r);
        notifyOwnerPaymentProofUploaded(reservationId, riderId, afterAttach);
        enqueueRiderReservationConfirmedAfterPaymentProof(riderId, afterAttach);
        LOGGER.atInfo()
                .addArgument(riderId)
                .addArgument(reservationId)
                .log("Rider riderId={} attached payment receipt for reservation id={}");
    }

    private void enqueueRiderReservationConfirmedAfterPaymentProof(final long riderId, final Reservation reservation) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<ListingAvailability> availabilityOpt = resolveAvailabilityForReservation(reservation);
            final Optional<User> listingOwnerOpt = resolveOwnerFromReservation(reservation);
            if (riderOpt.isEmpty() || listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log(
                        "Skipping rider confirmed-after-proof email: missing rider or owner (reservation id={})");
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final String vehicleLabel = resolveVehicleLabelFromReservation(reservation);
            final String riderLoc = availabilityOpt.map(a -> trimToNull(listingAddressFormatter.formatRiderReservationHandoverSummary(a, reservation))).orElse(null);
            final String ownerLoc = availabilityOpt.map(a -> trimToNull(listingAddressFormatter.formatOwnerReservationHandoverSummary(a))).orElse(null);
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .reservationId(reservation.getId())
                    .carId(reservation.getCarId())
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(riderLoc)
                    .ownerHandoverLocation(ownerLoc)
                    .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                    .ownerEmail(listingOwner.getEmail())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userService.resolveMailLocale(rider.getId()))
                    .ownerMailLocale(userService.resolveMailLocale(listingOwner.getId()))
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing rider reservation confirmed-after-proof email to {} for reservation id={}");
            emailService.sendRiderReservationConfirmedAfterPaymentProof(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId()).log("Could not enqueue rider confirmed-after-proof email for reservation id={}");
        }
    }

    private void notifyOwnerPaymentProofUploaded(final long reservationId, final long riderId, final Reservation reservation) {
        try {
            final Optional<User> ownerOpt = resolveOwnerFromReservation(reservation);
            final Optional<User> riderOpt = userService.getUserById(riderId);
            if (ownerOpt.isEmpty() || riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservationId).log("Skipping owner payment-proof email: missing owner or rider (reservation id={})");
                return;
            }
            final User owner = ownerOpt.get();
            final User rider = riderOpt.get();
            final String ownerFullName = owner.getForename() + " " + owner.getSurname();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final OwnerPaymentProofReceivedEmailPayload mailPayload = OwnerPaymentProofReceivedEmailPayload.builder()
                    .messageLocale(userService.resolveMailLocale(owner.getId()))
                    .recipientEmail(owner.getEmail())
                    .ownerFullName(ownerFullName)
                    .riderFullName(riderFullName)
                    .riderEmail(rider.getEmail())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .vehicleLabel(resolveVehicleLabelFromReservation(reservation))
                    .reservationId(reservationId)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .build();
            LOGGER.atInfo().addArgument(owner.getEmail()).addArgument(reservationId).log("Queueing owner payment-proof email to {} (reservation id={})");
            emailService.sendOwnerPaymentProofReceivedEmail(mailPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservationId).log("Could not enqueue owner payment-proof email for reservation id={}");
        }
    }

    private void enqueueOwnerRefundProofObligationEmail(final Reservation reservation, final boolean dueReminder) {
        try {
            if (!reservation.isPaymentRefundRequired()) {
                return;
            }
            final Optional<OffsetDateTime> deadlineOpt = reservation.getRefundProofDeadlineAt();
            if (deadlineOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping owner refund-proof email: no deadline (reservation id={})");
                return;
            }
            final Optional<User> ownerOpt = resolveOwnerFromReservation(reservation);
            if (ownerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping owner refund-proof email: owner not found (reservation id={})");
                return;
            }
            final User owner = ownerOpt.get();
            final String ownerFullName = owner.getForename() + " " + owner.getSurname();
            final OwnerRefundProofObligationEmailPayload mailPayload = OwnerRefundProofObligationEmailPayload.builder()
                    .messageLocale(userService.resolveMailLocale(owner.getId()))
                    .recipientEmail(owner.getEmail())
                    .ownerFullName(ownerFullName)
                    .vehicleLabel(resolveVehicleLabelFromReservation(reservation))
                    .reservationId(reservation.getId())
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .refundProofDeadlineAt(deadlineOpt.get())
                    .dueReminder(dueReminder)
                    .build();
            LOGGER.atInfo().addArgument(owner.getEmail()).addArgument(reservation.getId())
                    .log("Queueing owner refund-proof obligation email to {} (reservation id={})");
            emailService.sendOwnerRefundProofObligationEmail(mailPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId())
                    .log("Could not enqueue owner refund-proof obligation email for reservation id={}");
        }
    }

    private void notifyRiderRefundProofUploaded(final long reservationId, final Reservation reservation) {
        try {
            final Optional<User> ownerOpt = resolveOwnerFromReservation(reservation);
            final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
            if (ownerOpt.isEmpty() || riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservationId).log("Skipping rider refund-proof email: missing owner or rider (reservation id={})");
                return;
            }
            final User owner = ownerOpt.get();
            final User rider = riderOpt.get();
            final String ownerFullName = owner.getForename() + " " + owner.getSurname();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final RiderRefundProofReceivedEmailPayload mailPayload = RiderRefundProofReceivedEmailPayload.builder()
                    .messageLocale(userService.resolveMailLocale(rider.getId()))
                    .recipientEmail(rider.getEmail())
                    .riderFullName(riderFullName)
                    .ownerFullName(ownerFullName)
                    .ownerEmail(owner.getEmail())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .vehicleLabel(resolveVehicleLabelFromReservation(reservation))
                    .reservationId(reservationId)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservationId).log("Queueing rider refund-proof email to {} (reservation id={})");
            emailService.sendRiderRefundProofReceivedEmail(mailPayload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservationId).log("Could not enqueue rider refund-proof email for reservation id={}");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findPaymentReceiptForParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> asRider = getRiderReservationById(userId, reservationId);
        final Optional<Reservation> resOpt = asRider.isPresent()
                ? asRider
                : getOwnerReservationById(userId, reservationId);
        if (resOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = resOpt.get();
        final Optional<Long> fileIdOpt = r.getPaymentReceiptFileId();
        if (fileIdOpt.isEmpty()) {
            return Optional.empty();
        }
        final Optional<StoredFile> fileOpt = storedFileService.findById(fileIdOpt.get());
        if (fileOpt.isEmpty()) {
            return Optional.empty();
        }
        final StoredFile sf = fileOpt.get();
        if (sf.getUploaderUserId() != r.getRiderId()) {
            return Optional.empty();
        }
        return fileOpt;
    }

    @Override
    @Transactional
    public void attachRefundReceiptByOwner(
            final long ownerUserId,
            final long reservationId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        if (!StoredFile.isAllowedPaymentReceiptContentType(contentType)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        final int len = data == null ? 0 : data.length;
        if (len == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        if (len > paymentReceiptUploadPolicy.getMaxBytes()) {
            throw new RiderReservationException(
                    MessageKeys.RESERVATION_REFUND_RECEIPT_TOO_LARGE, paymentReceiptUploadPolicy.getMaxMegabytesRoundedUp());
        }
        final Reservation r = getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID));
        if (r.getStatus() != Reservation.Status.CANCELLED_BY_OWNER
                && r.getStatus() != Reservation.Status.CANCELLED_BY_RIDER) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        if (!r.isPaymentRefundRequired()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        if (r.getPaymentRefundReceiptFileId().isPresent()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        final StoredFile file = storedFileService.create(ownerUserId, originalFilename, contentType, data);
        final int updated = reservationDao.attachRefundReceipt(reservationId, ownerUserId, file.getId());
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_REFUND_RECEIPT_INVALID);
        }
        final Reservation after = getOwnerReservationById(ownerUserId, reservationId).orElse(r);
        notifyRiderRefundProofUploaded(reservationId, after);
        // Owner becomes eligible for auto-unblock as soon as no overdue refund proof remains.
        unblockOwnerIfNoMoreRefundOverdue(ownerUserId);
        LOGGER.atInfo()
                .addArgument(ownerUserId)
                .addArgument(reservationId)
                .log("Owner ownerUserId={} attached refund receipt for reservation id={}");
    }

    /**
     * Unblocks {@code ownerUserId} when no refund-proof deadline remains overdue. Safe to call after every
     * refund-proof upload; idempotent if the owner is not blocked.
     */
    private void unblockOwnerIfNoMoreRefundOverdue(final long ownerUserId) {
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            final long remaining = reservationDao.countOverdueRefundProofsForOwner(ownerUserId, now);
            if (remaining > 0L) {
                return;
            }
            final Optional<User> ownerOpt = userService.getUserById(ownerUserId);
            if (ownerOpt.isEmpty() || !ownerOpt.get().isBlocked()) {
                return;
            }
            userService.unblockUser(ownerUserId);
            LOGGER.atInfo().addArgument(ownerUserId).log("Auto-unblocked ownerId={} after all refund-proof debts cleared");
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(ownerUserId)
                    .log("Failed to evaluate owner auto-unblock after refund-proof upload (ownerId={})");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFile> findRefundReceiptForParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> asRider = getRiderReservationById(userId, reservationId);
        final Optional<Reservation> resOpt = asRider.isPresent()
                ? asRider
                : getOwnerReservationById(userId, reservationId);
        if (resOpt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = resOpt.get();
        final Optional<Long> fileIdOpt = r.getPaymentRefundReceiptFileId();
        if (fileIdOpt.isEmpty()) {
            return Optional.empty();
        }
        final Optional<StoredFile> fileOpt = storedFileService.findById(fileIdOpt.get());
        if (fileOpt.isEmpty()) {
            return Optional.empty();
        }
        final StoredFile sf = fileOpt.get();
        final Optional<User> ownerOpt = resolveOwnerFromReservation(r);
        if (ownerOpt.isEmpty() || sf.getUploaderUserId() != ownerOpt.get().getId()) {
            return Optional.empty();
        }
        return fileOpt;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getCarReservationCards(
            final long ownerId,
            final long carId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        return reservationDao.getCarReservationCards(ownerId, carId, page, pageSize, statusFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countCarReservationsByStatus(final long ownerId, final long carId) {
        return mergeCancelledBuckets(reservationDao.countCarReservationsByStatus(ownerId, carId));
    }

    /**
     * Keeps a single {@code cancelled} counter for owner dashboards while persisting granular statuses in the database.
     */
    private static Map<String, Long> mergeCancelledBuckets(final Map<String, Long> raw) {
        final Map<String, Long> out = new LinkedHashMap<>(raw);
        long merged = out.getOrDefault("cancelled", 0L);
        merged += out.getOrDefault("cancelled_by_rider", 0L);
        merged += out.getOrDefault("cancelled_by_owner", 0L);
        merged += out.getOrDefault("cancelled_due_to_missing_payment_proof", 0L);
        out.remove("cancelled_by_rider");
        out.remove("cancelled_by_owner");
        out.remove("cancelled_due_to_missing_payment_proof");
        out.put("cancelled", merged);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarTotalEarnings(final long ownerId, final long carId) {
        return reservationDao.sumCarRevenueByStatuses(
                ownerId, carId, Arrays.asList("accepted", "started", "finished"));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCarPendingEarnings(final long ownerId, final long carId) {
        return reservationDao.sumCarRevenueByStatuses(
                ownerId, carId, Arrays.asList("accepted", "started"));
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarTotalDaysRented(final long ownerId, final long carId) {
        return reservationDao.findCarFinishedReservations(ownerId, carId)
                .stream()
                .mapToLong(r -> calculateBillableDays(r.getStartDate(), r.getEndDate()))
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCarReservationsThisMonth(final long ownerId, final long carId) {
        final YearMonth current = YearMonth.now(ZoneOffset.UTC);
        final OffsetDateTime monthStart = current.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        final OffsetDateTime nextMonthStart = current.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return reservationDao.countCarReservationsCreatedBetween(ownerId, carId, monthStart, nextMonthStart);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getCarNextReservationDate(final long ownerId, final long carId) {
        return reservationDao.findCarNextActiveReservationDate(
                ownerId, carId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Sets {@link Reservation.Status#FINISHED} when the car is marked returned; if status was {@code finished} and
     * the car is no longer returned, reverts to {@link Reservation.Status#STARTED}.
     */
    private void syncFinishedStatusWithCarReturned(
            final long ownerUserId,
            final long reservationId) {
        final Optional<Reservation> freshOpt = getOwnerReservationById(ownerUserId, reservationId);
        if (freshOpt.isEmpty()) {
            return;
        }
        final Reservation fresh = freshOpt.get();
        final boolean carReturned = fresh.isCarReturned();
        if (carReturned
                && (fresh.getStatus() == Reservation.Status.ACCEPTED
                        || fresh.getStatus() == Reservation.Status.STARTED)) {
            reservationDao.updateReservationStatus(
                    reservationId, Reservation.Status.FINISHED.name().toLowerCase(Locale.ROOT));
        } else if (!carReturned && fresh.getStatus() == Reservation.Status.FINISHED) {
            reservationDao.updateReservationStatus(
                    reservationId, Reservation.Status.STARTED.name().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    @Transactional
    public void markCarReturnedByOwner(final long ownerUserId, final long reservationId) {
        final Reservation r = getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED));
        if (!OffsetDateTime.now(ZoneOffset.UTC).isAfter(r.getEndDate())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        if (r.getStatus() != Reservation.Status.ACCEPTED && r.getStatus() != Reservation.Status.STARTED) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        if (r.isCarReturned()) {
            return;
        }
        final int updated = reservationDao.markCarReturned(reservationId, ownerUserId);
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        syncFinishedStatusWithCarReturned(ownerUserId, reservationId);
        LOGGER.atInfo()
                .addArgument(ownerUserId)
                .addArgument(reservationId)
                .log("Owner ownerUserId={} marked car returned for reservation id={}");
    }

    @Override
    @Transactional
    public void dispatchReturnReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final int hours = reservationTimingPolicy.getReturnReminderHoursBeforeCheckout();
        final List<Reservation> candidates = reservationDao.findReservationsForReturnReminderEmail(now, hours);
        LOGGER.atInfo()
                .addArgument(candidates.size())
                .addArgument(hours)
                .log("Return reminder run: {} candidate reservation(s) (within {} h before checkout)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderCarReturnEmailPayload> payload = buildRiderCarReturnEmailPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping return reminder: missing data (reservation id={})");
                continue;
            }
            if (reservationDao.claimReturnReminderEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                emailService.sendRiderReturnReminderEmail(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Return reminder run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchReturnCheckoutEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationDao.findReservationsForReturnCheckoutEmail(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Return checkout email run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderCarReturnEmailPayload> payload = buildRiderCarReturnEmailPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping return checkout mail: missing data (reservation id={})");
                continue;
            }
            if (reservationDao.claimReturnCheckoutEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                emailService.sendRiderReturnCheckoutEmail(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return checkout email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Return checkout email run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchRiderReviewInviteEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationDao.findReservationsForRiderReviewInviteEmail(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Rider review invite run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<RiderReviewInviteEmailPayload> payload = buildRiderReviewInviteEmailPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping rider review invite: missing data (reservation id={})");
                continue;
            }
            if (reservationDao.claimRiderReviewInviteEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                emailService.sendRiderReviewInviteEmail(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue rider review invite email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Rider review invite run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchDuePaymentProofReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationDao.findReservationsWithDuePendingPaymentProof(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Due payment proof reminder run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            final Optional<ReservationMailPayload> payload = buildDuePaymentProofReminderEmailPayload(reservation);
            if (payload.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log("Skipping due payment proof reminder: missing data (reservation id={})");
                continue;
            }
            if (reservationDao.claimPendingPaymentProofEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                emailService.sendRiderDuePaymentProofEmail(payload.get());
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue due payment proof reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Due payment proof reminder run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void dispatchDueRefundProofReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> candidates = reservationDao.findReservationsWithDuePendingRefundProof(now);
        LOGGER.atInfo().addArgument(candidates.size()).log("Due refund proof reminder run: {} candidate reservation(s)");
        int queued = 0;
        for (final Reservation reservation : candidates) {
            if (reservationDao.claimPendingRefundEmailSent(reservation.getId()) == 0) {
                continue;
            }
            try {
                enqueueOwnerRefundProofObligationEmail(reservation, true);
                queued++;
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue due refund proof reminder email (reservation id={})");
            }
        }
        LOGGER.atInfo().addArgument(queued).log("Due refund proof reminder run: queued {} email(s)");
    }

    @Override
    @Transactional
    public void sweepRefundOverdueAndBlockOwners() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final List<Reservation> overdue = reservationDao.findReservationsWithOverdueRefundProof(now);
        LOGGER.atInfo().addArgument(overdue.size()).log("Refund-overdue sweep: {} reservation(s) with lapsed deadline");
        if (overdue.isEmpty()) {
            return;
        }
        // Group reservations by owner so we issue a single block + single email per owner.
        final java.util.Map<Long, List<Reservation>> byOwner = new java.util.LinkedHashMap<>();
        for (final Reservation r : overdue) {
            final Optional<User> ownerOpt = resolveOwnerFromReservation(r);
            if (ownerOpt.isEmpty()) {
                continue;
            }
            byOwner.computeIfAbsent(ownerOpt.get().getId(), k -> new java.util.ArrayList<>()).add(r);
        }
        int blockedCount = 0;
        for (final java.util.Map.Entry<Long, List<Reservation>> entry : byOwner.entrySet()) {
            final long ownerId = entry.getKey();
            try {
                final Optional<User> ownerOpt = userService.getUserById(ownerId);
                if (ownerOpt.isEmpty()) {
                    continue;
                }
                final User owner = ownerOpt.get();
                if (owner.isBlocked()) {
                    continue;
                }
                userService.blockUser(ownerId);
                blockedCount++;
                enqueueOwnerBlockedEmail(owner, entry.getValue());
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(ownerId)
                        .log("Failed to block ownerId={} during refund-overdue sweep");
            }
        }
        LOGGER.atInfo().addArgument(blockedCount).log("Refund-overdue sweep: blocked {} owner(s)");
    }

    private void enqueueOwnerBlockedEmail(final User owner, final List<Reservation> overdueReservations) {
        try {
            final List<ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload.OverdueRefundReservation> rows = new java.util.ArrayList<>();
            for (final Reservation r : overdueReservations) {
                final OffsetDateTime deadline = r.getRefundProofDeadlineAt().orElse(null);
                if (deadline == null) {
                    continue;
                }
                rows.add(new ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload.OverdueRefundReservation(
                        r.getId(),
                        resolveVehicleLabelFromReservation(r),
                        deadline,
                        ArsMoneyFormat.format(r.getTotalPrice())));
            }
            if (rows.isEmpty()) {
                return;
            }
            final ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload payload =
                    ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload.builder()
                            .messageLocale(userService.resolveMailLocale(owner.getId()))
                            .recipientEmail(owner.getEmail())
                            .ownerFullName(owner.getForename() + " " + owner.getSurname())
                            .overdueReservations(rows)
                            .build();
            emailService.sendOwnerBlockedEmail(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(owner.getId())
                    .log("Could not enqueue owner-blocked email for ownerId={}");
        }
    }

    private Optional<ReservationMailPayload> buildDuePaymentProofReminderEmailPayload(final Reservation reservation) {
        final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
        final Optional<User> ownerOpt = resolveOwnerFromReservation(reservation);
        if (riderOpt.isEmpty() || ownerOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final User owner = ownerOpt.get();
        final String vehicleLabel = resolveVehicleLabelFromReservation(reservation);
        
        // Retrieve owner's CBU; if not found, skip sending email and keep flag false
        String ownerCbu;
        try {
            ownerCbu = userService.getUserCbu(owner.getId());
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atDebug()
                    .setCause(e)
                    .addArgument(reservation.getId())
                    .addArgument(owner.getId())
                    .log("Skipping due payment proof reminder: owner CBU unavailable (reservation id={}, owner id={})");
            return Optional.empty();
        }
        
        final Locale riderLocale = userService.resolveMailLocale(rider.getId());
        return Optional.of(ReservationMailPayload.builder()
                .recipientEmail(rider.getEmail())
                .riderFullName(rider.getForename() + " " + rider.getSurname())
                .reservationId(reservation.getId())
                .carId(reservation.getCarId())
                .vehicleLabel(vehicleLabel)
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .ownerFullName(owner.getForename() + " " + owner.getSurname())
                .ownerEmail(owner.getEmail())
                .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                .ownerCbu(ownerCbu)
                .riderMailLocale(riderLocale)
                .ownerMailLocale(userService.resolveMailLocale(owner.getId()))
                .build());
    }

    private Optional<RiderCarReturnEmailPayload> buildRiderCarReturnEmailPayload(final Reservation reservation) {
        final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
        final Optional<ListingAvailability> availabilityOpt = resolveAvailabilityForReservation(reservation);
        final Optional<User> ownerOpt = resolveOwnerFromReservation(reservation);
        if (riderOpt.isEmpty() || ownerOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final User owner = ownerOpt.get();
        final Locale locale = userService.resolveMailLocale(rider.getId());
        final String checkout = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String returnLine = availabilityOpt
                .map(a -> listingAddressFormatter.formatPickupForReservationView(a, reservation, false))
                .orElse(null);
        final String path = "/my-reservations/" + reservation.getId();
        final String vehicleLabel = resolveVehicleLabelFromReservation(reservation);
        return Optional.of(RiderCarReturnEmailPayload.builder()
                .messageLocale(locale)
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .vehicleLabel(vehicleLabel)
                .ownerEmail(owner.getEmail())
                .checkoutFormatted(checkout)
                .returnLocationLine(returnLine)
                .reservationDetailPath(path)
                .build());
    }

    private Optional<RiderReviewInviteEmailPayload> buildRiderReviewInviteEmailPayload(final Reservation reservation) {
        final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
        if (riderOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final String vehicleLabel = resolveVehicleLabelFromReservation(reservation);
        final Locale locale = userService.resolveMailLocale(rider.getId());
        /* Deep link: must stay consistent with GET /my-reservations/{id} (default role=rider) + #rider-review-owner. */
        final String path = "/my-reservations/" + reservation.getId() + "?role=rider#rider-review-owner";
        return Optional.of(RiderReviewInviteEmailPayload.builder()
                .messageLocale(locale)
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .vehicleLabel(vehicleLabel)
                .reviewSectionPath(path)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByCarId(final long carId) {
        return reservationDao.findBlockingByCarId(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByCarIdInRange(
            final long carId, final OffsetDateTime from, final OffsetDateTime to) {
        return reservationDao.findBlockingByCarIdInRange(carId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findReminderReservations(final OffsetDateTime from, final OffsetDateTime to) {
        return reservationDao.getReminderReservations(from, to);
    }

    private static String trimName(final String forename, final String surname) {
        final String f = forename == null ? "" : forename.trim();
        final String s = surname == null ? "" : surname.trim();
        if (f.isEmpty()) {
            return s;
        }
        if (s.isEmpty()) {
            return f;
        }
        return f + " " + s;
    }
}
