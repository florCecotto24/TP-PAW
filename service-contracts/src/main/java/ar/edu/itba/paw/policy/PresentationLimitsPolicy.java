package ar.edu.itba.paw.policy;

/** Caps for teaser-style UI snippets (not SQL pagination windows). */
public interface PresentationLimitsPolicy {

    /** Comment-bearing reviews shown for counterparty sidebars ({@code GET /cars/{carId}} and {@code /my-reservations}). */
    int getCounterpartyRecentReviewsLimit();

    /** Max similar listing cards on {@code GET /cars/{carId}}. */
    int getCarDetailSimilarListingsLimit();

    /** Page size for counterparty profile "other active listings" grid and load-more batches. */
    int getCounterpartyOwnerActiveListingsPageSize();
}
