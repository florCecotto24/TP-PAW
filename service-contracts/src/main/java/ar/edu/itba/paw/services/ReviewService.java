package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.util.List;

import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;

/**
 * Post-rental ratings and comments tied to reservations (owner↔rider).
 * Implementations use {@code ReviewDao} only; reservation and owner rows are resolved through {@code ReservationService}
 * and {@code UserService}.
 */
public interface ReviewService {

    /** Maximum trimmed length for review comment text (configuration-backed). */
    int getReviewCommentMaxLength();

    /** Public listing page: paginated reviews with reviewer display fields. */
    Page<ListingPublicReview> getListingPublicReviews(long listingId, int page, int pageSize);

    /** Total public reviews stored for the listing. */
    long countReviewsForListing(long listingId);

    /** Like {@link #getListingPublicReviews} but resolves by {@code car_id} (Phase 7d+). */
    Page<ListingPublicReview> getCarPublicReviews(long carId, int page, int pageSize);

    /** Like {@link #countReviewsForListing} but resolves by {@code car_id} (Phase 7d+). */
    long countReviewsForCar(long carId);

    /**
     * Owner rates the rider after a completed rental; enforces reservation state and one review per side.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when validation or business rules fail
     */
    void submitOwnerReviewOfRider(long ownerUserId, long reservationId, Integer rating, String comment);

    /**
     * Rider rates the owner after a completed rental; enforces reservation state and one review per side.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when validation or business rules fail
     */
    void submitRiderReviewOfOwner(long riderUserId, long reservationId, Integer rating, String comment);

    /** Whether the owner already left a review for this reservation. */
    boolean hasOwnerReview(long reservationId);

    /** Whether the rider already left a review for this reservation. */
    boolean hasRiderReview(long reservationId);

    /**
     * Average rating (1–5 scale) for the given user as owner or rider, from reviews where they were rated
     * ({@code counterpartyIsOwner} selects which side of the reservation is the rated party).
     */
    BigDecimal getAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    /** Recent reviews that include a non-blank comment, for profile counterparty snippets. */
    List<ReviewItemDto> getRecentCommentReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner, int limit);
}
