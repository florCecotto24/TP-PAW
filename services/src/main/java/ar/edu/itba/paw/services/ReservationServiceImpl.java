package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationConfirmationPayload;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ReservationDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Log LOG = LogFactory.getLog(ReservationServiceImpl.class);
    private static final long MINUTES_PER_DAY = 24L * 60L;

    private final ReservationDao reservationDao;
    private final ListingService listingService;
    private final ListingAvailabilityDao listingAvailabilityDao;
    private final UserService userService;
    private final EmailService emailService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ListingService listingService,
            final ListingAvailabilityDao listingAvailabilityDao,
            final UserService userService,
            final EmailService emailService) {
        this.reservationDao = reservationDao;
        this.listingService = listingService;
        this.listingAvailabilityDao = listingAvailabilityDao;
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
                    listingOwner.getEmail());
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
            final String email,
            final String name,
            final String surname,
            final Long listingId,
            final Long availabilityId,
            final String fromDateTime,
            final String untilDateTime) {
        if (listingId == null || listingService.getListingById(listingId).isEmpty()) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND);
        }
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
        if (!listingService.reservationIntervalFitsListingAvailability(listingId, availabilityId, startDate, endDate)) {
            throw new RiderReservationException(MessageKeys.RESERVATION_RIDER_OUTSIDE_AVAILABILITY);
        }
        final User rider = userService.findOrCreatePublisher(email, name, surname);
        final Reservation reservation = createReservation(
                rider.getId(),
                listingId,
                startDate,
                endDate,
                Reservation.Status.ACCEPTED);
        consumeAvailabilityForReservation(listingId, startDate, endDate);
        return reservation;
    }

    private void consumeAvailabilityForReservation(
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        final LocalDate p = startDate.toInstant().atZone(wall).toLocalDate();
        final LocalDate r = endDate.toInstant().atZone(wall).toLocalDate();
        final List<ListingAvailability> active = new ArrayList<>(listingAvailabilityDao.findByListingId(listingId));
        if (!everyWallDayCoveredByAvailabilities(p, r, active)) {
            throw new ReservationConflictException(MessageKeys.RESERVATION_CONFLICT_OVERLAP);
        }
        final List<ListingAvailability> overlapping = active.stream()
                .filter(a -> !a.getEndInclusive().isBefore(p) && !a.getStartInclusive().isAfter(r))
                .sorted(Comparator.comparing(ListingAvailability::getStartInclusive))
                .collect(Collectors.toList());
        for (final ListingAvailability row : overlapping) {
            consumeAvailabilitySliceOnRow(listingId, row, p, r);
        }
        mergeAdjacentActiveAvailabilities(listingId);
    }

    private static boolean everyWallDayCoveredByAvailabilities(
            final LocalDate pickupDay,
            final LocalDate returnDay,
            final List<ListingAvailability> active) {
        LocalDate d = pickupDay;
        while (!d.isAfter(returnDay)) {
            final LocalDate day = d;
            final boolean covered = active.stream().anyMatch(
                    a -> !day.isBefore(a.getStartInclusive()) && !day.isAfter(a.getEndInclusive()));
            if (!covered) {
                return false;
            }
            d = d.plusDays(1);
        }
        return true;
    }

    private void consumeAvailabilitySliceOnRow(
            final long listingId,
            final ListingAvailability row,
            final LocalDate reservationStart,
            final LocalDate reservationEnd) {
        final LocalDate a = row.getStartInclusive();
        final LocalDate b = row.getEndInclusive();
        final LocalDate overlapStart = a.isAfter(reservationStart) ? a : reservationStart;
        final LocalDate overlapEnd = b.isBefore(reservationEnd) ? b : reservationEnd;
        if (overlapStart.isAfter(overlapEnd)) {
            return;
        }
        if (row.getListingId() != listingId) {
            throw new IllegalStateException("listing_availability.listing_id mismatch");
        }
        listingAvailabilityDao.setActive(row.getId(), false);
        if (a.isBefore(overlapStart)) {
            listingAvailabilityDao.create(listingId, a, overlapStart.minusDays(1));
        }
        if (b.isAfter(overlapEnd)) {
            listingAvailabilityDao.create(listingId, overlapEnd.plusDays(1), b);
        }
    }

    private void mergeAdjacentActiveAvailabilities(final long listingId) {
        while (true) {
            final List<ListingAvailability> active = listingAvailabilityDao.findByListingId(listingId);
            if (active.size() < 2) {
                return;
            }
            boolean merged = false;
            for (int i = 0; i < active.size() - 1; i++) {
                final ListingAvailability a = active.get(i);
                final ListingAvailability b = active.get(i + 1);
                if (!b.getStartInclusive().isAfter(a.getEndInclusive().plusDays(1))) {
                    final LocalDate newEnd = a.getEndInclusive().isBefore(b.getEndInclusive())
                            ? b.getEndInclusive()
                            : a.getEndInclusive();
                    listingAvailabilityDao.updateDateRange(a.getId(), a.getStartInclusive(), newEnd);
                    listingAvailabilityDao.setActive(b.getId(), false);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                return;
            }
        }
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

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
