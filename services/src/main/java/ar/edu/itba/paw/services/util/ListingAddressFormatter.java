package ar.edu.itba.paw.services.util;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.services.LocationService;

/**
 * Builds human-readable pickup and return address lines for a listing. Combines street, optional door number, and
 * neighborhood label from {@link LocationService}. Intended for JSPs, reservation detail views, and email payloads;
 * does not read or write listing persistence. Transaction boundaries are provided by callers (e.g. {@code ListingViewServiceImpl});
 * this type stays {@code final} and is not proxied for AOP.
 */
@Component
public final class ListingAddressFormatter {

    private final LocationService locationService;

    /**
     * @param locationService catalog lookup for neighborhood names by id
     */
    @Autowired
    public ListingAddressFormatter(final LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Return leg for public contexts: same rules as {@link #formatPublicPickupLocation(Listing)} when pickup and return share one place.
     *
     * @param listing listing whose start point defines the address
     * @return street and neighborhood without door number, trimmed segments
     */
    public String formatPublicDeliveryLocation(final Listing listing) {
        return formatPublicPickupLocation(listing);
    }

    /**
     * Return leg for trusted contexts: same line as {@link #formatFullPickupLocation(Listing)} when pickup and return share one place.
     *
     * @param listing listing whose start point defines the address
     * @return street, optional number, and neighborhood when available
     */
    public String formatFullDeliveryLocation(final Listing listing) {
        return formatFullPickupLocation(listing);
    }

    /**
     * Pickup line without door number: street plus neighborhood name when the listing has a neighborhood id resolved in the catalog.
     *
     * @param listing listing row (street and optional neighborhood id)
     * @return non-null string; may be blank if both street and neighborhood name are missing
     */
    public String formatPublicPickupLocation(final Listing listing) {
        return formatPublicPickupLocation(
                listing.getStartPointStreet(),
                listing.getNeighborhoodId().orElse(null));
    }

    /**
     * Same as {@link #formatPublicPickupLocation(Listing)} but reads street and neighborhood directly
     * from a {@link ListingAvailability} row (used when {@code Listing} entity is unavailable).
     */
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

    /**
     * Pickup line with door number when present, then neighborhood name from the catalog.
     *
     * @param listing listing row (street, optional number, optional neighborhood id)
     * @return non-null string; may be blank if all parts are missing
     */
    public String formatFullPickupLocation(final Listing listing) {
        final String street = listing.getStartPointStreet() == null ? "" : listing.getStartPointStreet().trim();
        final Optional<String> numberOpt = listing.getStartPointNumber();
        final String streetWithNumber;
        if (numberOpt.isPresent() && !numberOpt.get().isBlank()) {
            streetWithNumber = street.isBlank() ? numberOpt.get().trim() : street + " " + numberOpt.get().trim();
        } else {
            streetWithNumber = street;
        }
        if (listing.getNeighborhoodId().isEmpty()) {
            return streetWithNumber;
        }
        final String neighborhoodName = locationService.findNeighborhoodById(listing.getNeighborhoodId().get())
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

    /** Whether the rider may see the full address including door number (payment proof uploaded or approved). */
    private static boolean riderSeesSensitiveAddressNumbers(final Reservation reservation) {
        return reservation.getPaymentReceiptFileId().isPresent() || reservation.isPaymentApproved();
    }

    /**
     * Return leg for a reservation screen: full line for the owner or for the rider once proof rules allow; otherwise public line.
     *
     * @param listing     listing address source
     * @param reservation used to decide if the rider already unlocked the full address
     * @param viewerIsOwner when {@code true}, always show the full delivery line
     * @return formatted return location text
     */
    public String formatDeliveryForReservationView(
            final Listing listing,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullDeliveryLocation(listing);
        }
        return formatPublicDeliveryLocation(listing);
    }

    /**
     * Pickup leg for a reservation screen: full line for the owner or for the rider after proof; otherwise public pickup line.
     *
     * @param listing       listing address source
     * @param reservation   payment state for rider visibility of the number
     * @param viewerIsOwner when {@code true}, always show the full pickup line
     * @return formatted pickup location text
     */
    public String formatPickupForReservationView(
            final Listing listing,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullPickupLocation(listing);
        }
        return formatPublicPickupLocation(listing);
    }

    /**
     * Single-line handover summary for rider-facing email: uses rider visibility rules (no door number until proof).
     * Collapses to one segment when pickup and return lines are equal or one side is blank.
     *
     * @param listing     listing address
     * @param reservation reservation whose payment state gates the rider view
     * @return combined pickup and return text, separated by {@code " · "} when both differ
     */
    public String formatRiderReservationHandoverSummary(final Listing listing, final Reservation reservation) {
        final String p = formatPickupForReservationView(listing, reservation, false);
        final String d = formatDeliveryForReservationView(listing, reservation, false);
        if (p.isBlank()) {
            return d;
        }
        if (d.isBlank() || p.trim().equals(d.trim())) {
            return p;
        }
        return p + " · " + d;
    }

    /**
     * Single-line handover summary for owner-facing email: always full pickup and full return lines.
     * Collapses to one segment when both sides are equal or one is blank.
     *
     * @param listing listing address
     * @return combined pickup and return text, separated by {@code " · "} when both differ
     */
    public String formatOwnerReservationHandoverSummary(final Listing listing) {
        final String p = formatFullPickupLocation(listing);
        final String d = formatFullDeliveryLocation(listing);
        if (p.isBlank()) {
            return d;
        }
        if (d.isBlank() || p.trim().equals(d.trim())) {
            return p;
        }
        return p + " · " + d;
    }

    /** Like {@link #formatFullPickupLocation(Listing)} but reads from a {@link ListingAvailability} row. */
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

    /** Like {@link #formatFullDeliveryLocation(Listing)} but reads from a {@link ListingAvailability} row. */
    public String formatFullDeliveryLocation(final ListingAvailability availability) {
        return formatFullPickupLocation(availability);
    }

    /** Like {@link #formatPickupForReservationView(Listing, Reservation, boolean)} but reads from {@link ListingAvailability}. */
    public String formatPickupForReservationView(
            final ListingAvailability availability,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullPickupLocation(availability);
        }
        return formatPublicPickupLocation(availability);
    }

    /** Like {@link #formatDeliveryForReservationView(Listing, Reservation, boolean)} but reads from {@link ListingAvailability}. */
    public String formatDeliveryForReservationView(
            final ListingAvailability availability,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullDeliveryLocation(availability);
        }
        return formatPublicPickupLocation(availability);
    }

    /** Like {@link #formatRiderReservationHandoverSummary(Listing, Reservation)} but reads from {@link ListingAvailability}. */
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

    /** Like {@link #formatOwnerReservationHandoverSummary(Listing)} but reads from {@link ListingAvailability}. */
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
