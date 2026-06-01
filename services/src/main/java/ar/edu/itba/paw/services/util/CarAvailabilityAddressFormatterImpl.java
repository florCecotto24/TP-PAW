package ar.edu.itba.paw.services.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.services.LocationService;

/**
 * {@link CarAvailabilityAddressFormatter} implementation backed by {@link LocationService} for neighborhood lookups.
 * Transaction boundaries are provided by callers; this type stays {@code final} and is not proxied for AOP.
 */
@Component
public final class CarAvailabilityAddressFormatterImpl implements CarAvailabilityAddressFormatter {

    private final LocationService locationService;

    @Autowired
    public CarAvailabilityAddressFormatterImpl(final LocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    public String formatPublicPickupLocation(final CarAvailability availability) {
        return formatPublicPickupLocation(
                availability.getStartPointStreet(),
                availability.getNeighborhoodId().orElse(null));
    }

    private String formatPublicPickupLocation(final String startPointStreet, final Long neighborhoodId) {
        final String street = startPointStreet == null ? "" : startPointStreet.trim();
        if (neighborhoodId == null) {
            return street;
        }
        final String neighborhoodName = locationService.findNeighborhoodById(neighborhoodId)
                .map(Neighborhood::getName)
                .orElse("");
        if (neighborhoodName.isBlank()) {
            return street;
        }
        if (street.isBlank()) {
            return neighborhoodName.trim();
        }
        return street + ", " + neighborhoodName.trim();
    }

    /** Whether the rider may see the full address including door number (payment proof uploaded). */
    private static boolean riderSeesSensitiveAddressNumbers(final Reservation reservation) {
        return reservation.getPaymentReceiptFileId().isPresent();
    }

    @Override
    public String formatFullPickupLocation(final CarAvailability availability) {
        final String street = availability.getStartPointStreet() == null ? "" : availability.getStartPointStreet().trim();
        final Optional<String> numberOpt = availability.getStartPointNumber();
        final String streetWithNumber;
        if (numberOpt.isPresent() && !numberOpt.get().isBlank()) {
            streetWithNumber = street.isBlank() ? numberOpt.get().trim() : street + " " + numberOpt.get().trim();
        } else {
            streetWithNumber = street;
        }
        if (availability.getNeighborhoodId().isEmpty()) {
            return streetWithNumber;
        }
        final String neighborhoodName = locationService.findNeighborhoodById(availability.getNeighborhoodId().get())
                .map(Neighborhood::getName)
                .orElse("");
        if (neighborhoodName.isBlank()) {
            return streetWithNumber;
        }
        if (streetWithNumber.isBlank()) {
            return neighborhoodName.trim();
        }
        return streetWithNumber + ", " + neighborhoodName.trim();
    }

    @Override
    public String formatFullDeliveryLocation(final CarAvailability availability) {
        return formatFullPickupLocation(availability);
    }

    @Override
    public String formatPublicDeliveryLocation(final CarAvailability availability) {
        return formatPublicPickupLocation(availability);
    }

    @Override
    public String formatPickupForReservationView(
            final CarAvailability availability,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullPickupLocation(availability);
        }
        return formatPublicPickupLocation(availability);
    }

    @Override
    public String formatDeliveryForReservationView(
            final CarAvailability availability,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullDeliveryLocation(availability);
        }
        return formatPublicPickupLocation(availability);
    }

    @Override
    public String formatRiderReservationHandoverSummary(
            final CarAvailability availability, final Reservation reservation) {
        final String p = formatPickupForReservationView(availability, reservation, false);
        final String d = formatDeliveryForReservationView(availability, reservation, false);
        if (p.isBlank()) {
            return d;
        }
        if (d.isBlank() || p.trim().equals(d.trim())) {
            return p;
        }
        return p + " · " + d;
    }

    @Override
    public String formatOwnerReservationHandoverSummary(final CarAvailability availability) {
        final String p = formatFullPickupLocation(availability);
        final String d = formatFullDeliveryLocation(availability);
        if (p.isBlank()) {
            return d;
        }
        if (d.isBlank() || p.trim().equals(d.trim())) {
            return p;
        }
        return p + " · " + d;
    }
}
