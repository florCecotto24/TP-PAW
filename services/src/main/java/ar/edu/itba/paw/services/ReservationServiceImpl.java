package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
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
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ReservationDao;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Log LOG = LogFactory.getLog(ReservationServiceImpl.class);
    private static final long MINUTES_PER_DAY = 24L * 60L;

    private final ReservationDao reservationDao;
    private final ListingService listingService;
    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ListingService listingService,
            final UserService userService,
            final EmailService emailService) {
        this.reservationDao = reservationDao;
        this.listingService = listingService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @Override
    public Reservation createReservation(
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        if (reservationDao.hasActiveOverlap(listingId, startDate, endDate)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
        if (listingOwnerOpt.isPresent() && listingOwnerOpt.get().getId() == riderId) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING);
        }
        final String deliveryLocation = listingService.getListingById(listingId)
                .map(Listing::getStartPoint)
                .orElse(null);
        final Reservation reservation =
                reservationDao.createReservation(riderId, listingId, startDate, endDate, status);
        enqueueReservationConfirmationEmail(riderId, listingId, reservation, deliveryLocation);
        return reservation;
    }

    private void enqueueReservationConfirmationEmail(
            final long riderId,
            final long listingId,
            final Reservation reservation,
            final String deliveryLocation) {
        try {
            final Optional<User> riderOpt = userService.getUserById(riderId);
            final Optional<User> listingOwnerOpt = userService.getListingOwner(listingId);
            final Optional<Listing> listingOpt = listingService.getListingById(listingId);
            if (riderOpt.isEmpty()) {
                LOG.warn("Skipping reservation confirmation email: user not found for riderId=" + riderId
                        + " reservationId=" + reservation.getId());
                return;
            }
            if (listingOpt.isEmpty()) {
                LOG.warn("Skipping reservation confirmation email: listing not found for listingId=" + listingId
                        + " reservationId=" + reservation.getId());
                return;
            }
            if (listingOwnerOpt.isEmpty()) {
                LOG.warn("Skipping reservation confirmation email: listing owner not found for listingId=" + listingId
                        + " reservationId=" + reservation.getId());
                return;
            }
            final User rider = riderOpt.get();
            final User listingOwner = listingOwnerOpt.get();
            final Listing listing = listingOpt.get();
            final String vehicleLabel = listing.getTitle();
            final String riderFullName = rider.getForename() + " " + rider.getSurname();
            final String trimmedDelivery =
                    deliveryLocation == null || deliveryLocation.isBlank() ? null : deliveryLocation.trim();
            final ReservationConfirmationPayload payload = new ReservationConfirmationPayload(
                    rider.getEmail(),
                    riderFullName,
                    reservation.getId(),
                    listingId,
                    vehicleLabel,
                    reservation.getStartDate(),
                    reservation.getEndDate(),
                    trimmedDelivery,
                    listingOwner.getForename() + " " + listingOwner.getSurname(),
                    listingOwner.getEmail(),
                    resolveMailMessageLocale());
            LOG.info("Queueing reservation confirmation email to " + rider.getEmail()
                    + " for reservation id=" + reservation.getId());
            emailService.sendReservationConfirmationEmail(payload);
        } catch (final Exception e) {
            LOG.error("Could not enqueue reservation confirmation email for reservation id=" + reservation.getId(), e);
        }
    }

    @Override
    public Optional<Reservation> getReservationById(final long id) {
        return reservationDao.getReservationById(id);
    }

    @Override
    public Optional<Reservation> getRiderReservationById(final long riderId, final long reservationId) {
        return reservationDao.getReservationById(reservationId)
                .filter(reservation -> reservation.getRiderId() == riderId);
    }

    @Override
    public Page<ReservationCard> getRiderReservationCards(final long riderId, final int page, final int pageSize) {
        return reservationDao.getRiderReservationCards(riderId, page, pageSize);
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
        validateReservationPickupNotBeforeToday(startDate);
        if (!listingService.reservationIntervalFitsListingAvailability(listingId, availabilityId, startDate, endDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY);
        }
        return createReservation(
                rider.getId(),
                listingId,
                startDate,
                endDate,
                Reservation.Status.ACCEPTED);
    }

    @Override
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
        final long totalMinutes = Duration.between(startDate, endDate).toMinutes();
        return (totalMinutes + MINUTES_PER_DAY - 1L) / MINUTES_PER_DAY;
    }

    private String formatMoney(final BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static void validateReservationPickupNotBeforeToday(final OffsetDateTime startDate) {
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final LocalDate startDay = startDate.atZoneSameInstant(AvailabilityPeriod.WALL_ZONE).toLocalDate();
        if (startDay.isBefore(today)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_DATES_NOT_FROM_TODAY);
        }
    }

    private static Locale resolveMailMessageLocale() {
        final Locale locale = LocaleContextHolder.getLocale();
        if (locale != null && "es".equalsIgnoreCase(locale.getLanguage())) {
            return Locale.forLanguageTag("es");
        }
        return Locale.ENGLISH;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
