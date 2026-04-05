package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class RiderReservationServiceImpl implements RiderReservationService {

    private static final String MSG_LISTING_NOT_FOUND = "We could not find the listing you are trying to reserve.";
    private static final String MSG_DATES_REQUIRED = "Select pickup and return date/time before confirming.";
    private static final String MSG_DATES_INVALID_FORMAT = "Invalid date/time format. Please choose the dates again.";
    private static final String MSG_END_NOT_AFTER_START = "Return date/time must be after pickup date/time.";
    private static final String MSG_OUTSIDE_AVAILABILITY = "Selected pickup/return is outside the listing availability.";

    private final ListingService listingService;
    private final UserService userService;
    private final ReservationService reservationService;
    private final ReservationPricingService reservationPricingService;

    @Autowired
    public RiderReservationServiceImpl(
            final ListingService listingService,
            final UserService userService,
            final ReservationService reservationService,
            final ReservationPricingService reservationPricingService) {
        this.listingService = listingService;
        this.userService = userService;
        this.reservationService = reservationService;
        this.reservationPricingService = reservationPricingService;
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
            return reservationPricingService.calculateTotal(listingId, startDate, endDate)
                    .map(this::formatMoney);
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
            throw new RiderReservationException(MSG_LISTING_NOT_FOUND);
        }
        if (isBlank(fromDateTime) || isBlank(untilDateTime)) {
            throw new RiderReservationException(MSG_DATES_REQUIRED);
        }
        final OffsetDateTime startDate;
        final OffsetDateTime endDate;
        try {
            startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(fromDateTime);
            endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc(untilDateTime);
        } catch (final DateTimeParseException e) {
            throw new RiderReservationException(MSG_DATES_INVALID_FORMAT);
        }
        if (!endDate.isAfter(startDate)) {
            throw new RiderReservationException(MSG_END_NOT_AFTER_START);
        }
        if (!listingService.reservationIntervalFitsListingAvailability(listingId, availabilityId, startDate, endDate)) {
            throw new RiderReservationException(MSG_OUTSIDE_AVAILABILITY);
        }
        final User rider = userService.findOrCreatePublisher(email, name, surname);
        return reservationService.createReservation(
                rider.getId(),
                listingId,
                startDate,
                endDate,
                Reservation.Status.ACCEPTED);
    }

    private String formatMoney(final BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
