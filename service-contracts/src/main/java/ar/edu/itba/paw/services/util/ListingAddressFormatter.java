package ar.edu.itba.paw.services.util;

import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;

/**
 * Builds human-readable pickup and return address lines from a {@link ListingAvailability} row.
 * Combines street, optional door number, and neighborhood label resolved from the location service.
 * Intended for JSPs, reservation detail views, and email payloads; does not read or write persistence.
 */
public interface ListingAddressFormatter {

    /** Public pickup line (street + neighborhood, no door number) read from a {@link ListingAvailability}. */
    String formatPublicPickupLocation(ListingAvailability availability);

    /** Full pickup line (street, optional number, neighborhood) read from a {@link ListingAvailability}. */
    String formatFullPickupLocation(ListingAvailability availability);

    /** Return leg mirrors {@link #formatFullPickupLocation(ListingAvailability)} when pickup and return share one place. */
    String formatFullDeliveryLocation(ListingAvailability availability);

    /** Return leg mirrors {@link #formatPublicPickupLocation(ListingAvailability)}. */
    String formatPublicDeliveryLocation(ListingAvailability availability);

    /** Pickup line for the reservation view; honours owner/rider visibility rules. */
    String formatPickupForReservationView(
            ListingAvailability availability, Reservation reservation, boolean viewerIsOwner);

    /** Return line for the reservation view; honours owner/rider visibility rules. */
    String formatDeliveryForReservationView(
            ListingAvailability availability, Reservation reservation, boolean viewerIsOwner);

    /** Single-line handover summary for rider-facing emails; collapses when pickup and return share one place. */
    String formatRiderReservationHandoverSummary(ListingAvailability availability, Reservation reservation);

    /** Single-line handover summary for owner-facing emails; always uses the full pickup/return lines. */
    String formatOwnerReservationHandoverSummary(ListingAvailability availability);
}
