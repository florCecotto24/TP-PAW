package ar.edu.itba.paw.models;

import java.util.List;

/**
 * Resultado de cargar en una sola consulta las tarjetas destacadas del home
 * (más baratas y más recientes).
 */
public record HomeListingCards(List<ListingCard> cheapest, List<ListingCard> mostRecent) {
}
