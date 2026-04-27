package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;

public interface ReviewService {

    int getReviewCommentMaxLength();

    Page<ListingPublicReview> getListingPublicReviews(long listingId, int page, int pageSize);

    long countReviewsForListing(long listingId);

    void submitOwnerReviewOfRider(long ownerUserId, long reservationId, Integer rating, String comment);

    void submitRiderReviewOfOwner(long riderUserId, long reservationId, Integer rating, String comment);

    boolean hasOwnerReview(long reservationId);

    boolean hasRiderReview(long reservationId);
}
