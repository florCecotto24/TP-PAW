package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.OwnerListingDetailPageModel;

/**
 * Read-only API for listing-related UI: owner dashboard page model and human-readable pickup/delivery lines for
 * public pages, reservation views, and transactional email payloads.
 * Mutating listing workflows stay on {@link ListingService}. Implementations assemble {@link OwnerListingDetailPageModel}
 * using {@link ReservationService} and catalog reads; pickup/delivery strings are delegated to a dedicated address
 * formatter (services layer, {@link LocationService} only, no listing DAO).
 */
public interface ListingViewService {

    /**
     * Builds the full page model for an owner's listing detail/edit view from an already-loaded {@link ListingDetail}.
     *
     * @param detail loaded listing with owner, car, pictures, and availabilities
     * @param locale locale for human-readable dates and money formatting
     * @return model ready to expose to the view layer; not {@code null}
     */
    OwnerListingDetailPageModel buildOwnerListingDetailPageModel(ListingDetail detail, Locale locale);

    /**
     * Finds the active or paused listing for the given car and builds the owner detail page model.
     * Returns empty when the car has no active or paused listing.
     */
    Optional<OwnerListingDetailPageModel> buildOwnerCarDetailPageModel(long carId, Locale locale);

    /**
     * Returns the minimum effective day price across all non-expired availability periods.
     * If a period overrides the listing price, that price is considered; otherwise the listing default is used.
     * Returns the listing-level day price when no future periods have a period-specific price.
     */
    BigDecimal resolveMinEffectiveDayPrice(ListingDetail detail);

    /**
     * Returns {@code true} when at least one non-expired availability period has a per-period price
     * that differs from the listing's default day price.
     */
    boolean isListingPriceVariable(ListingDetail detail);

    /**
     * Same address as pickup (street + neighborhood, no door number), for public views.
     */
    String formatPublicDeliveryLocation(Listing listing);

    /**
     * Street + neighborhood of pickup (without door number), for public views.
     */
    String formatPublicPickupLocation(Listing listing);

    /**
     * Same as {@link #formatFullPickupLocation(Listing)} when pickup and return share one address line.
     */
    String formatFullDeliveryLocation(Listing listing);

    /** Pickup address with street number and neighborhood (full line for trusted views). */
    String formatFullPickupLocation(Listing listing);

    /**
     * Return leg: full line for owner or rider after proof; otherwise same as {@link #formatPublicDeliveryLocation(Listing)}.
     */
    String formatDeliveryForReservationView(Listing listing, Reservation reservation, boolean viewerIsOwner);

    /**
     * Pickup line: full address for owner or rider after proof; otherwise same as {@link #formatPublicPickupLocation(Listing)}.
     */
    String formatPickupForReservationView(Listing listing, Reservation reservation, boolean viewerIsOwner);

    /**
     * Compact pickup/return summary for rider-facing email (hides door number until payment proof or approval).
     */
    String formatRiderReservationHandoverSummary(Listing listing, Reservation reservation);

    /**
     * Compact pickup/return summary for owner-facing email (always includes full address).
     */
    String formatOwnerReservationHandoverSummary(Listing listing);
}
