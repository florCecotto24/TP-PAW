package ar.edu.itba.paw.util;

import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Reservation;

/**
 * Builds human-readable pickup and return address lines from a {@link CarAvailability} row.
 * Combines street, optional door number, and neighborhood label resolved from the location service.
 * Intended for JSPs, reservation detail views, and email payloads; does not read or write persistence.
 */
public interface CarAvailabilityAddressFormatter {

    /** Public pickup line (street + neighborhood, no door number) read from a {@link CarAvailability}. */
    String formatPublicPickupLocation(CarAvailability availability);

    /** Full pickup line (street, optional number, neighborhood) read from a {@link CarAvailability}. */
    String formatFullPickupLocation(CarAvailability availability);

    /** Return leg mirrors {@link #formatFullPickupLocation(CarAvailability)} when pickup and return share one place. */
    String formatFullDeliveryLocation(CarAvailability availability);

    /** Return leg mirrors {@link #formatPublicPickupLocation(CarAvailability)}. */
    String formatPublicDeliveryLocation(CarAvailability availability);

    /** Pickup line for the reservation view; honours owner/rider visibility rules. */
    String formatPickupForReservationView(
            CarAvailability availability, Reservation reservation, boolean viewerIsOwner);

    /** Return line for the reservation view; honours owner/rider visibility rules. */
    String formatDeliveryForReservationView(
            CarAvailability availability, Reservation reservation, boolean viewerIsOwner);

    /** Single-line handover summary for rider-facing emails; collapses when pickup and return share one place. */
    String formatRiderReservationHandoverSummary(CarAvailability availability, Reservation reservation);

    /** Single-line handover summary for owner-facing emails; always uses the full pickup/return lines. */
    String formatOwnerReservationHandoverSummary(CarAvailability availability);
}
