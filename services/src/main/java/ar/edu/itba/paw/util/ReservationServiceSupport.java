package ar.edu.itba.paw.util;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.services.reservation.ReservationAvailabilityService;

import ar.edu.itba.paw.services.reservation.ReservationPaymentServiceImpl;
import ar.edu.itba.paw.services.reservation.ReservationServiceImpl;
import ar.edu.itba.paw.services.reservation.ReservationWorkflowServiceImpl;
/**
 * Reservation-related helpers shared by {@code ReservationWorkflowServiceImpl} and
 * {@code ReservationPaymentServiceImpl}. Kept here so the two services that resulted from
 * splitting the legacy {@code ReservationServiceImpl} do not need to duplicate small
 * formatting / resolution methods or the same {@code ReservationAvailabilityService} round-trip.
 */
@Component
public final class ReservationServiceSupport {

    private final ReservationAvailabilityService reservationAvailabilityService;

    @Autowired
    public ReservationServiceSupport(
            final ReservationAvailabilityService reservationAvailabilityService) {
        this.reservationAvailabilityService = reservationAvailabilityService;
    }

    /** Resolves the car owner from a reservation by reading the {@code Car} entity directly. */
    public Optional<User> resolveOwnerFromReservation(final Reservation reservation) {
        return Optional.ofNullable(reservation.getCar()).map(Car::getOwner);
    }

    /**
     * Snapshot availability row driving pickup display for the reservation: among the bridged
     * candidates that OFFERED-cover the reservation's first wall-calendar day, the one with the
     * latest {@code createdAt} wins. Anchors the pickup info shown in transactional emails to
     * the rows captured at pricing time, so later owner edits do not mutate the address / times
     * the rider sees for an already-bridged reservation.
     */
    public Optional<CarAvailability> resolveAvailabilityForReservation(final Reservation reservation) {
        return reservationAvailabilityService
                .findEffectivePickupAvailabilityForReservation(reservation.getId());
    }

    /**
     * Human-readable vehicle label using the car's brand and model. Falls back to an empty
     * string when the reservation has no associated car.
     */
    public String resolveVehicleLabelFromReservation(final Reservation reservation) {
        return Optional.ofNullable(reservation.getCar())
                .map(c -> c.getBrand() + " " + c.getModel())
                .orElse("");
    }

    /** Single-line handover address built from the availability start point. */
    public static String formatHandoverLocation(final CarAvailability availability) {
        if (availability == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(availability.getStartPointStreet());
        availability.getStartPointNumber().ifPresent(n -> sb.append(" ").append(n));
        return sb.toString().trim();
    }

    public static String trimToNull(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String trimName(final String forename, final String surname) {
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
