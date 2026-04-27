package ar.edu.itba.paw.models.dto;

import java.util.List;

public record HomeListingCards(List<ListingCard> cheapest, List<ListingCard> mostRecent) {
}
