package ar.edu.itba.paw.persistence;

import java.util.Optional;

import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;

public interface ReviewDao {

    boolean existsReview(long reservationId, boolean madeByRider);

    void insertReview(long reservationId, boolean madeByRider, int rating, String comment);

    Page<ListingPublicReview> findListingPublicReviews(long listingId, int page, int pageSize);

    void refreshRiderAverageRating(long riderUserId);

    void refreshOwnerAverageRating(long ownerUserId);

    void refreshListingRatingAvg(long listingId);

    long countReviewsForListing(long listingId);
}
