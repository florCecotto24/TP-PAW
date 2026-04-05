package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ReservationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class ReservationServiceImpl implements ReservationService {

    private static final long MINUTES_PER_DAY = 24L * 60L;

    private final ReservationDao reservationDao;
    private final ListingService listingService;
    private final UserService userService;

    @Autowired
    public ReservationServiceImpl(
            final ReservationDao reservationDao,
            final ListingService listingService,
            final UserService userService) {
        this.reservationDao = reservationDao;
        this.listingService = listingService;
        this.userService = userService;
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
        return reservationDao.createReservation(riderId, listingId, startDate, endDate, status);
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

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
