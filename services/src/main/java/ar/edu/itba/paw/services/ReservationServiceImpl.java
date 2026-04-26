package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationCard;
import ar.edu.itba.paw.models.ReservationConfirmationPayload;
import ar.edu.itba.paw.models.StoredFile;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ReservationDao;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationDao reservationDao;
    private final ListingService listingService;
    private final UserService userService;
    private final EmailService emailService;
    private final StoredFileService storedFileService;
    private final ImageService imageService;
    private final ReservationTimingPolicy reservationTimingPolicy;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ListingService listingService,
            final UserService userService,
            final EmailService emailService,
            final StoredFileService storedFileService,
            final ImageService imageService,
            final ReservationTimingPolicy reservationTimingPolicy) {
        this.reservationDao = reservationDao;
        this.listingService = listingService;
        this.userService = userService;
        this.emailService = emailService;
        this.storedFileService = storedFileService;
        this.imageService = imageService;
        this.reservationTimingPolicy = reservationTimingPolicy;
    }

    @Override
    public int getConfiguredPickupLeadHours() {
        return reservationTimingPolicy.getPickupLeadHours();
    }

    @Override
    public int getConfiguredPaymentProofDeadlineHours() {
        return reservationTimingPolicy.getPaymentProofDeadlineHours();
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
        if (reservationDao.hasActiveOverlap(listingId, startDate, endDate)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
        if (listingOwnerOpt.isPresent() && listingOwnerOpt.get().getId() == riderId) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING);
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
                .map(l -> listingService.formatRiderReservationHandoverSummary(l, reservation))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        final String ownerLoc = listingForMail
                .map(listingService::formatOwnerReservationHandoverSummary)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        enqueueReservationConfirmationEmail(riderId, listingId, reservation, riderLoc, ownerLoc);
        return reservation;
    }

    private void enqueueReservationConfirmationEmail(
            final long riderId,
            final long listingId,
            final Reservation reservation,
            final String riderHandoverLocation,
            final String ownerHandoverLocation) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
            final Optional<Listing> listingOpt = listingService.getListingById(listingId);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().log("Skipping reservation confirmation email: user not found for riderId=" + riderId
                        + " reservationId=" + reservation.getId());
                return;
            }
            if (listingOpt.isEmpty()) {
                LOGGER.atWarn().log("Skipping reservation confirmation email: listing not found for listingId=" + listingId
                        + " reservationId=" + reservation.getId());
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().log("Skipping reservation confirmation email: listing owner not found for listingId=" + listingId
                        + " reservationId=" + reservation.getId());
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
            final ReservationConfirmationPayload payload = new ReservationConfirmationPayload(
                    rider.getEmail(),
                    riderFullName,
                    reservation.getId(),
                    listingId,
                    vehicleLabel,
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    trimmedRiderLoc,
                    trimmedOwnerLoc,
                    listingOwner.getForename() + " " + listingOwner.getSurname(),
                    listingOwner.getEmail(),
                    reservation.getTotalPrice().toString(),
                    userService.resolveMailLocale(rider.getId()),
                    userService.resolveMailLocale(listingOwner.getId()));
            LOGGER.atInfo().log("Queueing reservation confirmation email to " + rider.getEmail()
                    + " for reservation id=" + reservation.getId());
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final Exception e) {
            LOGGER.atError().log("Could not enqueue reservation confirmation email for reservation id=" + reservation.getId(), e);
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
    public Page<ReservationCard> getRiderReservationCards(final long riderId, final int page, final int pageSize, final String statusFilter) {
        return reservationDao.getRiderReservationCards(riderId, page, pageSize, statusFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> getOwnerReservationCards(final long ownerId, final int page, final int pageSize, final String statusFilter) {
        return reservationDao.getOwnerReservationCards(ownerId, page, pageSize, statusFilter);
    }

    @Override
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
            return calculateTotal(listingId, startDate, endDate).map(this::formatMoney);
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

    private Optional<Reservation> performCancellationAndNotify(final long reservationId) {
        reservationDao.updateReservationStatus(reservationId, Reservation.Status.CANCELLED.name().toLowerCase());
        final Optional<Reservation> reservationOpt = reservationDao.getReservationById(reservationId);
        if (reservationOpt.isPresent()) {
            enqueueCancellationEmail(reservationId, reservationOpt.get());
        }
        return reservationOpt;
    }

    @Override
    @Transactional
    public Optional<Reservation> cancelReservation(final long reservationId) {
        return performCancellationAndNotify(reservationId);
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
        final boolean isOwner = userService.getListingOwner(r.getListingId())
                .map(o -> o.getId() == userId)
                .orElse(false);
        if (!isRider && !isOwner) {
            return Optional.empty();
        }
        return switch (r.getStatus()) {
            case PENDING -> {
                if (r.getPaymentReceiptFileId().isPresent()) {
                    yield Optional.empty();
                }
                yield performCancellationAndNotify(reservationId);
            }
            case ACCEPTED -> performCancellationAndNotify(reservationId);
            default -> Optional.empty();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForCancellation(final long listingId) {
        return reservationDao.getListingActiveReservations(listingId);
    }


    private void enqueueCancellationEmail(
            final long reservationId,
            final Reservation reservation) {
        try {
            final long riderId = reservation.getRiderId();
            final long listingId = reservation.getListingId();
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
            final Optional<Listing> listingOpt = listingService.getListingById(listingId);
            if (riderOpt.isEmpty()) {
                LOGGER.atWarn().log("Skipping reservation cancellation email: user not found for riderId=" + riderId
                        + " reservationId=" + reservationId);
                return;
            }
            if (listingOpt.isEmpty()) {
                LOGGER.atWarn().log("Skipping reservation cancellation email: listing not found for listingId=" + listingId
                        + " reservationId=" + reservationId);
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOGGER.atWarn().log("Skipping reservation cancellation email: listing owner not found for listingId=" + listingId
                        + " reservationId=" + reservationId);
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final Listing listing = listingOpt.get();
            final String vehicleLabel = listing.getTitle();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String riderLoc = trimToNull(listingService.formatRiderReservationHandoverSummary(listing, reservation));
            final String ownerLoc = trimToNull(listingService.formatOwnerReservationHandoverSummary(listing));
            final ReservationConfirmationPayload payload = new ReservationConfirmationPayload(
                    rider.getEmail(),
                    riderFullName,
                    reservation.getId(),
                    listingId,
                    vehicleLabel,
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    riderLoc,
                    ownerLoc,
                    listingOwner.getForename() + " " + listingOwner.getSurname(),
                    listingOwner.getEmail(),
                    reservation.getTotalPrice().toString(),
                    userService.resolveMailLocale(rider.getId()),
                    userService.resolveMailLocale(listingOwner.getId()));
            LOGGER.atInfo().log("Queueing reservation cancellation email to " + rider.getEmail()
                    + " for reservation id=" + reservationId);
            emailService.sendReservationCancellationEmail(payload);
        } catch (final Exception e) {
            LOGGER.atError().log("Could not enqueue reservation cancellation email for reservation id=" + reservationId, e);
        }
    }

    @Override
    @Transactional
    public void cancelExpiredPendingPaymentReservations() {
        for (final Reservation r : reservationDao.findPendingPaymentPastDeadline(OffsetDateTime.now(ZoneOffset.UTC))) {
            cancelReservation(r.getId());
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
        if (len > imageService.getMaxImageBytes()) {
            throw new RiderReservationException(
                    MessageKeys.RESERVATION_PAYMENT_RECEIPT_TOO_LARGE, imageService.getMaxImageMegabytesRoundedUp());
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
        return reservationDao.countListingReservationsByStatus(ownerId, listingId);
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
}
