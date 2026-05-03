package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Locale;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.email.OwnerPaymentProofReceivedEmailPayload;
import ar.edu.itba.paw.models.email.ReservationCancellationEmailPayload;
import ar.edu.itba.paw.models.email.ReservationMailPayload;
import ar.edu.itba.paw.models.email.RiderCarReturnEmailPayload;
import ar.edu.itba.paw.models.email.RiderReviewInviteEmailPayload;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.util.ReservationSearchCriteria;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

/**
 * Reservation workflows: persists through {@link ReservationDao} only.
 * Listings, users, stored files, images, and mail go through {@link ListingService}, {@link ListingViewService}
 * (pickup/delivery lines for mail and payloads), {@link UserService}, {@link StoredFileService}, {@link ImageService},
 * and {@link EmailService}; timing and pagination use policy beans.
 */
@Service
public final class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationDao reservationDao;
    private final ListingService listingService;
    private final ListingViewService listingViewService;
    private final UserService userService;
    private final EmailService emailService;
    private final StoredFileService storedFileService;
    private final ImageService imageService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;
    private final PaginationPolicy paginationPolicy;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ListingService listingService,
            @Lazy final ListingViewService listingViewService,
            final UserService userService,
            final EmailService emailService,
            final StoredFileService storedFileService,
            final ImageService imageService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final PaymentReceiptUploadPolicy paymentReceiptUploadPolicy,
            final PaginationPolicy paginationPolicy) {
        this.reservationDao = reservationDao;
        this.listingService = listingService;
        this.listingViewService = listingViewService;
        this.userService = userService;
        this.emailService = emailService;
        this.storedFileService = storedFileService;
        this.imageService = imageService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.paymentReceiptUploadPolicy = paymentReceiptUploadPolicy;
        this.paginationPolicy = paginationPolicy;
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
    @Transactional
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final OffsetDateTime paymentProofDeadlineAt) {
        final long billableDays = calculateBillableDays(startDate, endDate);
        if (billableDays > reservationTimingPolicy.getMaxBillableDaysPerReservation()) {
            throw new RiderReservationException(
                    MessageKeys.RESERVATION_RIDER_MAX_BILLABLE_DAYS,
                    reservationTimingPolicy.getMaxBillableDaysPerReservation());
        }
        if (reservationDao.hasActiveOverlap(listingId, startDate, endDate)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
        if (listingOwnerOpt.isEmpty()) {
            throw new RiderReservationException(MessageKeys.USER_ACCOUNT_NOT_FOUND);
        }
        long ownerId = listingOwnerOpt.get().getId();
        if (ownerId == riderId) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING);
        }
        final String cbu;
        try {
            cbu = userService.getUserCbu(ownerId);
        } catch (final UserNotFoundException | CBUNotFoundException e) {
            LOGGER.atWarn().setCause(e).addArgument(ownerId).log("Owner payment details unavailable for ownerId={}");
            throw new RiderReservationException(MessageKeys.RESERVATION_OWNER_PAYMENT_DETAILS_UNAVAILABLE);
        }
        final String reservationTotal = calculateTotal(listingId, startDate, endDate).map(this::formatMoney)
                .orElse(null);
        if (reservationTotal == null) {
            throw new RiderReservationException(MessageKeys.RESERVATION_TOTAL_PRICE_INVALID);
        }
        final Reservation reservation =
                reservationDao.createReservation(
                        riderId, listingId, startDate, endDate, status, new BigDecimal(reservationTotal), paymentProofDeadlineAt);
        listingService.refreshListingFinishedIfExhausted(listingId);
        final Optional<Listing> listingForMail = listingService.getListingById(listingId);
        final String riderLoc = listingForMail
                .map(l -> listingViewService.formatRiderReservationHandoverSummary(l, reservation))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        final String ownerLoc = listingForMail
                .map(listingViewService::formatOwnerReservationHandoverSummary)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        enqueueReservationConfirmationEmail(riderId, listingId, reservation, riderLoc, ownerLoc, cbu);
        return reservation;
    }

    private void enqueueReservationConfirmationEmail(
            final long riderId,
            final long listingId,
            final Reservation reservation,
            final String riderHandoverLocation,
            final String ownerHandoverLocation,
            final String cbu) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
            final Optional<Listing> listingOpt = listingService.getListingById(listingId);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(riderId).addArgument(reservation.getId()).log("Skipping reservation confirmation email: user not found for riderId={} reservationId={}");
                return;
            }
            if (listingOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(listingId).addArgument(reservation.getId()).log("Skipping reservation confirmation email: listing not found for listingId={} reservationId={}");
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(listingId).addArgument(reservation.getId()).log("Skipping reservation confirmation email: listing owner not found for listingId={} reservationId={}");
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final Listing listing = listingOpt.get();
            final String vehicleLabel = listing.getTitle();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String trimmedRiderLoc =
                    riderHandoverLocation == null || riderHandoverLocation.isBlank() ? null : riderHandoverLocation.trim();
            final String trimmedOwnerLoc =
                    ownerHandoverLocation == null || ownerHandoverLocation.isBlank() ? null : ownerHandoverLocation.trim();
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(riderFullName)
                    .reservationId(reservation.getId())
                    .listingId(listingId)
                    .vehicleLabel(vehicleLabel)
                    .startDate(reservation.getStartDate())
                    .endDate(reservation.getEndDate())
                    .riderHandoverLocation(trimmedRiderLoc)
                    .ownerHandoverLocation(trimmedOwnerLoc)
                    .ownerFullName(listingOwner.getForename() + " " + listingOwner.getSurname())
                    .ownerEmail(listingOwner.getEmail())
                    .reservationTotal(ArsMoneyFormat.format(reservation.getTotalPrice()))
                    .riderMailLocale(userService.resolveMailLocale(rider.getId()))
                    .ownerMailLocale(userService.resolveMailLocale(listingOwner.getId()))
                    .ownerCbu(cbu)
                    .build();
            LOGGER.atInfo().addArgument(rider.getEmail()).addArgument(reservation.getId())
                    .log("Queueing reservation confirmation email to {} for reservation id={}");
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).addArgument(reservation.getId()).log("Could not enqueue reservation confirmation email for reservation id={}");
        }
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
            final String textQuery) {
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
                ownerId, riderId, page, paginationPolicy.getDefaultPageSize(), statuses,
                carTypes, transmissions, powertrains, minPrice, maxPrice, ratingBands, sortBy, sortDir, textQuery);
    }

    private static List<String> collectCarTypeParams(final List<String> raw) {
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (raw == null) {
            return out;
        }
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
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    private static List<String> collectTransmissionParams(final List<String> raw) {
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Transmission.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    private static List<String> collectPowertrainParams(final List<String> raw) {
        final java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Powertrain.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    @Override
    @Transactional
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
    public Optional<String> reservationTotalDisplay(
            final Long listingId,
            final String fromDateTime,
            final String untilDateTime) {
        if (listingId == null || isBlank(fromDateTime) || isBlank(untilDateTime)) {
            return Optional.empty();
        }
        try {
            final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(fromDateTime);
            final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(untilDateTime);
            return calculateTotal(listingId, startDate, endDate).map(ArsMoneyFormat::format);
        } catch (final DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Reservation submitRiderReservation(
            final long riderId,
            final Long listingId,
            final Long availabilityId,
            final String fromDateTime,
            final String untilDateTime) {
        if (listingId == null || listingService.getListingById(listingId).isEmpty()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND);
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
        if (!listingService.reservationIntervalFitsListingAvailability(listingId, availabilityId, startDate, endDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY);
        }
        final OffsetDateTime proofDeadline = OffsetDateTime.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPaymentProofDeadlineHours(), ChronoUnit.HOURS),
                ZoneOffset.UTC);
        return createReservation(
                rider.getId(),
                listingId,
                startDate,
                endDate,
                Reservation.Status.PENDING,
                proofDeadline);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BigDecimal> calculateTotal(
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final long billableDays = calculateBillableDays(startDate, endDate);
        if (billableDays <= 0) {
            return Optional.empty();
        }
        return listingService.getListingById(listingId)
                .map(Listing::getDayPrice)
                .map(dayPrice -> dayPrice.multiply(BigDecimal.valueOf(billableDays)));
    }

    @Override
    @Transactional
    public long calculateBillableDays(final OffsetDateTime startDate, final OffsetDateTime endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return 0;
        }
        final LocalDate pickupDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        final LocalDate returnDay = endDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        return Math.max(1L, ChronoUnit.DAYS.between(pickupDay, returnDay.plusDays(1)));
    }

    private String formatMoney(final BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
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
        reservationOpt.ifPresent(r -> enqueueCancellationEmail(r));
        return reservationOpt;
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservation(final long reservationId) {
        return performCancellationAndNotify(
                reservationId, Reservation.Status.CANCELLED_DUE_TO_MISSING_PAYMENT_PROOF);
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservationAsParticipant(final long userId, final long reservationId) {
        final Optional<Reservation> opt = reservationDao.getReservationById(reservationId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        final Reservation r = opt.get();
        if (r.getPaymentReceiptFileId().isPresent()) {
            return Optional.empty();
        }
        final boolean isRider = r.getRiderId() == userId;
        final boolean isOwner = userService.getListingOwner(r.getListingId())
                .map(o -> o.getId() == userId)
                .orElse(false);
        if (!isRider && !isOwner) {
            return Optional.empty();
        }
        final Reservation.Status cancelledStatus =
                isRider ? Reservation.Status.CANCELLED_BY_RIDER : Reservation.Status.CANCELLED_BY_OWNER;
        return switch (r.getStatus()) {
            case PENDING, ACCEPTED -> performCancellationAndNotify(reservationId, cancelledStatus);
            default -> Optional.empty();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForCancellation(final long listingId) {
        return reservationDao.getListingActiveReservations(listingId);
    }


    private void enqueueCancellationEmail(final Reservation reservation) {
        try {
            final long riderId = reservation.getRiderId();
            final long listingId = reservation.getListingId();
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
            final Optional<Listing> listingOpt = listingService.getListingById(listingId);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(riderId).addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: user not found for riderId={} reservationId={}");
                return;
            }
            if (listingOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(listingId).addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: listing not found for listingId={} reservationId={}");
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(listingId).addArgument(reservation.getId())
                        .log("Skipping reservation cancellation email: listing owner not found for listingId={} reservationId={}");
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final Listing listing = listingOpt.get();
            final String vehicleLabel = listing.getTitle();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String riderLoc = trimToNull(listingViewService.formatRiderReservationHandoverSummary(listing, reservation));
            final String ownerLoc = trimToNull(listingViewService.formatOwnerReservationHandoverSummary(listing));
            final ReservationMailPayload mail = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(riderFullName)
                    .reservationId(reservation.getId())
                    .listingId(listingId)
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
        for (final Reservation r : reservationDao.findPendingPaymentPastDeadline(OffsetDateTime.now(ZoneOffset.UTC))) {
            performCancellationAndNotify(r.getId(), status);
        }
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
        listingService.refreshListingFinishedIfExhausted(r.getListingId());
        final Reservation afterAttach = getRiderReservationById(riderId, reservationId).orElse(r);
        notifyOwnerPaymentProofUploaded(reservationId, riderId, afterAttach);
        enqueueRiderReservationConfirmedAfterPaymentProof(riderId, afterAttach);
    }

    private void enqueueRiderReservationConfirmedAfterPaymentProof(final long riderId, final Reservation reservation) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
            final Optional<User> listingOwnerOpt = userService.getListingOwner(reservation.getListingId());
            if (riderOpt.isEmpty() || listingOpt.isEmpty() || listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservation.getId()).log(
                        "Skipping rider confirmed-after-proof email: missing rider, listing, or owner (reservation id={})");
                return;
            }
            final User rider = riderOpt.get();
            final Listing listing = listingOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final String riderLoc = trimToNull(listingViewService.formatRiderReservationHandoverSummary(listing, reservation));
            final String ownerLoc = trimToNull(listingViewService.formatOwnerReservationHandoverSummary(listing));
            final ReservationMailPayload payload = ReservationMailPayload.builder()
                    .recipientEmail(rider.getEmail())
                    .riderFullName(rider.getForename() + " " + rider.getSurname())
                    .reservationId(reservation.getId())
                    .listingId(listing.getId())
                    .vehicleLabel(listing.getTitle())
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
            final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
            final Optional<User> ownerOpt = userService.getListingOwner(reservation.getListingId());
            final Optional<User> riderOpt = userService.getUserById(riderId);
            if (listingOpt.isEmpty() || ownerOpt.isEmpty() || riderOpt.isEmpty()) {
                LOGGER.atWarn().addArgument(reservationId).log("Skipping owner payment-proof email: missing listing or owner or rider (reservation id={})");
                return;
            }
            final Listing listing = listingOpt.get();
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
                    .vehicleLabel(listing.getTitle())
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
    public void setPaymentReceiptApprovalByOwner(
            final long ownerUserId,
            final long reservationId,
            final boolean approved) {
        final Reservation r = getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_APPROVAL_INVALID));
        if (r.getStatus() != Reservation.Status.ACCEPTED) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_APPROVAL_INVALID);
        }
        if (r.getPaymentReceiptFileId().isEmpty()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_APPROVAL_INVALID);
        }
        final int updated = reservationDao.updatePaymentApproved(reservationId, ownerUserId, approved);
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_PAYMENT_APPROVAL_INVALID);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getListingReservationCards(
            final long ownerId,
            final long listingId,
            final int page,
            final int pageSize,
            final String statusFilter) {
        return reservationDao.getListingReservationCards(ownerId, listingId, page, pageSize, statusFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countListingReservationsByStatus(final long ownerId, final long listingId) {
        return mergeCancelledBuckets(reservationDao.countListingReservationsByStatus(ownerId, listingId));
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
    public BigDecimal getListingTotalEarnings(final long ownerId, final long listingId) {
        return reservationDao.sumListingRevenueByStatuses(
                ownerId, listingId, Arrays.asList("accepted", "started", "finished"));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getListingPendingEarnings(final long ownerId, final long listingId) {
        return reservationDao.sumListingRevenueByStatuses(
                ownerId, listingId, Arrays.asList("accepted", "started"));
    }

    @Override
    @Transactional(readOnly = true)
    public long getListingTotalDaysRented(final long ownerId, final long listingId) {
        return reservationDao.findListingFinishedReservations(ownerId, listingId)
                .stream()
                .mapToLong(r -> calculateBillableDays(r.getStartDate(), r.getEndDate()))
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public long getListingReservationsThisMonth(final long ownerId, final long listingId) {
        final YearMonth current = YearMonth.now(ZoneOffset.UTC);
        final OffsetDateTime monthStart = current.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        final OffsetDateTime nextMonthStart = current.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return reservationDao.countListingReservationsCreatedBetween(ownerId, listingId, monthStart, nextMonthStart);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OffsetDateTime> getListingNextReservationDate(final long ownerId, final long listingId) {
        return reservationDao.findListingNextActiveReservationDate(
                ownerId, listingId, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional
    public void markCarReturnedByOwner(final long ownerUserId, final long reservationId) {
        final Reservation r = getOwnerReservationById(ownerUserId, reservationId)
                .orElseThrow(() -> new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED));
        if (r.getStatus() != Reservation.Status.ACCEPTED
                && r.getStatus() != Reservation.Status.STARTED
                && r.getStatus() != Reservation.Status.FINISHED) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        if (!OffsetDateTime.now(ZoneOffset.UTC).isAfter(r.getEndDate())) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
        if (r.isCarReturned()) {
            return;
        }
        final int updated = reservationDao.markCarReturned(reservationId, ownerUserId);
        if (updated == 0) {
            throw new RiderReservationException(MessageKeys.RESERVATION_MARK_RETURNED_NOT_ALLOWED);
        }
    }

    @Override
    @Transactional
    public void dispatchReturnReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final int hours = reservationTimingPolicy.getReturnReminderHoursBeforeCheckout();
        for (final Reservation reservation : reservationDao.findReservationsForReturnReminderEmail(now, hours)) {
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
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return reminder email (reservation id={})");
            }
        }
    }

    @Override
    @Transactional
    public void dispatchReturnCheckoutEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (final Reservation reservation : reservationDao.findReservationsForReturnCheckoutEmail(now)) {
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
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue return checkout email (reservation id={})");
            }
        }
    }

    @Override
    @Transactional
    public void dispatchRiderReviewInviteEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (final Reservation reservation : reservationDao.findReservationsForRiderReviewInviteEmail(now)) {
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
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue rider review invite email (reservation id={})");
            }
        }
    }

    @Override
    @Transactional
    public void dispatchDuePaymentProofReminderEmails() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        for (final Reservation reservation : reservationDao.findReservationsWithDuePendingPaymentProof(now)) {
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
            } catch (final RuntimeException e) {
                LOGGER.atError().setCause(e).addArgument(reservation.getId())
                        .log("Failed to queue due payment proof reminder email (reservation id={})");
            }
        }
    }

    private Optional<ReservationMailPayload> buildDuePaymentProofReminderEmailPayload(final Reservation reservation) {
        final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
        final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
        final Optional<User> ownerOpt = userService.getListingOwner(reservation.getListingId());
        if (riderOpt.isEmpty() || listingOpt.isEmpty() || ownerOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final User owner = ownerOpt.get();
        final Listing listing = listingOpt.get();
        
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
                .listingId(listing.getId())
                .vehicleLabel(listing.getTitle())
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
        final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
        final Optional<User> ownerOpt = userService.getListingOwner(reservation.getListingId());
        if (riderOpt.isEmpty() || listingOpt.isEmpty() || ownerOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final Listing listing = listingOpt.get();
        final User owner = ownerOpt.get();
        final Locale locale = userService.resolveMailLocale(rider.getId());
        final String checkout = WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(reservation.getEndDate(), locale);
        final String returnLine = listingViewService.formatPickupForReservationView(listing, reservation, false);
        final String path = "/my-reservations/" + reservation.getId();
        return Optional.of(RiderCarReturnEmailPayload.builder()
                .messageLocale(locale)
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .vehicleLabel(listing.getTitle())
                .ownerEmail(owner.getEmail())
                .checkoutFormatted(checkout)
                .returnLocationLine(returnLine)
                .reservationDetailPath(path)
                .build());
    }

    private Optional<RiderReviewInviteEmailPayload> buildRiderReviewInviteEmailPayload(final Reservation reservation) {
        final Optional<User> riderOpt = userService.getUserById(reservation.getRiderId());
        final Optional<Listing> listingOpt = listingService.getListingById(reservation.getListingId());
        if (riderOpt.isEmpty() || listingOpt.isEmpty()) {
            return Optional.empty();
        }
        final User rider = riderOpt.get();
        final Listing listing = listingOpt.get();
        final Locale locale = userService.resolveMailLocale(rider.getId());
        /* Deep link: must stay consistent with GET /my-reservations/{id} (default role=rider) + #rider-review-owner. */
        final String path = "/my-reservations/" + reservation.getId() + "?role=rider#rider-review-owner";
        return Optional.of(RiderReviewInviteEmailPayload.builder()
                .messageLocale(locale)
                .recipientEmail(rider.getEmail())
                .riderFullName(trimName(rider.getForename(), rider.getSurname()))
                .vehicleLabel(listing.getTitle())
                .reviewSectionPath(path)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByListingId(final long listingId) {
        return reservationDao.findBlockingByListingId(listingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findBlockingReservationsByListingIds(final Collection<Long> listingIds) {
        return reservationDao.findBlockingByListingIds(listingIds);
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
