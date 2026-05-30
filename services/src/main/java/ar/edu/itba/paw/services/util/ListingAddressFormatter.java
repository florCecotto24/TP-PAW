package ar.edu.itba.paw.services.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.services.LocationService;

/**
 * Builds human-readable pickup and return address lines from a {@link ListingAvailability} row.
 * Combines street, optional door number, and neighborhood label from {@link LocationService}.
 * Intended for JSPs, reservation detail views, and email payloads; does not read or write persistence.
 * Transaction boundaries are provided by callers; this type stays {@code final} and is not proxied for AOP.
 */
@Component
public final class ListingAddressFormatter {

    private final LocationService locationService;

    @Autowired
    public ListingAddressFormatter(final LocationService locationService) {
        this.locationService = locationService;
    }

    /** Public pickup line (street + neighborhood, no door number) read from a {@link ListingAvailability}. */
    public String formatPublicPickupLocation(final ListingAvailability availability) {
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

    /** Full pickup line (street, optional number, neighborhood) read from a {@link ListingAvailability}. */
    public String formatFullPickupLocation(final ListingAvailability availability) {
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

    /** Return leg mirrors {@link #formatFullPickupLocation(ListingAvailability)} when pickup and return share one place. */
    public String formatFullDeliveryLocation(final ListingAvailability availability) {
        return formatFullPickupLocation(availability);
    }

    /** Return leg mirrors {@link #formatPublicPickupLocation(ListingAvailability)}. */
    public String formatPublicDeliveryLocation(final ListingAvailability availability) {
        return formatPublicPickupLocation(availability);
    }

    /** Pickup line for the reservation view; honours owner/rider visibility rules. */
    public String formatPickupForReservationView(
            final ListingAvailability availability,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullPickupLocation(availability);
        }
        return formatPublicPickupLocation(availability);
    }

    /** Return line for the reservation view; honours owner/rider visibility rules. */
    public String formatDeliveryForReservationView(
            final ListingAvailability availability,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullDeliveryLocation(availability);
        }
        return formatPublicPickupLocation(availability);
    }

    /** Single-line handover summary for rider-facing emails; collapses when pickup and return share one place. */
    public String formatRiderReservationHandoverSummary(
            final ListingAvailability availability, final Reservation reservation) {
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

    /** Single-line handover summary for owner-facing emails; always uses the full pickup/return lines. */
    public String formatOwnerReservationHandoverSummary(final ListingAvailability availability) {
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
