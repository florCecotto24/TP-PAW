package ar.edu.itba.paw.services.policy;

/** Caps for teaser-style UI snippets (not SQL pagination windows). */
public interface PresentationLimitsPolicy {

    /** Comment-bearing reviews shown for counterparty sidebars (/car-detail and /my-reservations). */
    int getCounterpartyRecentReviewsLimit();

    /** Max similar listing cards on {@code /car-detail}. */
    int getCarDetailSimilarListingsLimit();

    /** Page size for counterparty profile "other active listings" grid and load-more batches. */
    int getCounterpartyOwnerActiveListingsPageSize();
}
