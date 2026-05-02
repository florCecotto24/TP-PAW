package ar.edu.itba.paw.models.dto;

import java.util.List;

/** Home page teaser buckets: cheapest listings and most recently published listings. */
public record HomeListingCards(List<ListingCard> cheapest, List<ListingCard> mostRecent) {
}
