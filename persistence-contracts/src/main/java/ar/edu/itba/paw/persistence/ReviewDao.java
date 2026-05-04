package ar.edu.itba.paw.persistence;

import java.util.List;
import java.math.BigDecimal;

import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;

/** Reviews per reservation and aggregated rating maintenance. */
public interface ReviewDao {

    boolean existsReview(long reservationId, boolean madeByRider);

    /** {@code rating} {@code null} persists an omitted review (same PK slot; excluded from public aggregates). */
    void insertReview(long reservationId, boolean madeByRider, Integer rating, String comment);

    Page<ListingPublicReview> findListingPublicReviews(long listingId, int page, int pageSize);

    void refreshRiderAverageRating(long riderUserId);

    void refreshOwnerAverageRating(long ownerUserId);

    void refreshListingRatingAvg(long listingId);

    long countReviewsForListing(long listingId);

    BigDecimal findAverageRatingForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner);

    List<ReviewItemDto> findRecentCommentReviewsForCounterparty(long counterpartyUserId, boolean counterpartyIsOwner, int limit);
}
