package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.dto.ListingCard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HomeListingCardsTest {

    @Test
    void testRecordProvidesEqualsHashCodeAndToString() {
        // Arrange
        final ListingCard card = new ListingCard(1L, 1L, "Toyota", "Yaris", new BigDecimal("100.00"), 10L);
        final HomeListingCards first = new HomeListingCards(List.of(card), List.of(card));
        final HomeListingCards second = new HomeListingCards(List.of(card), List.of(card));
        // Exercise & Assert
        Assertions.assertEquals(first, second);
        Assertions.assertEquals(first.hashCode(), second.hashCode());
        Assertions.assertEquals(
                "HomeListingCards[cheapest=[ar.edu.itba.paw.models.dto.ListingCard@" + Integer.toHexString(card.hashCode())
                        + "], mostRecent=[ar.edu.itba.paw.models.dto.ListingCard@" + Integer.toHexString(card.hashCode())
                        + "]]",
                first.toString());
    }

    @Test
    void testRecordKeepsProvidedListReferences() {
        // Arrange
        final List<ListingCard> cheapest = new ArrayList<>();
        final List<ListingCard> mostRecent = new ArrayList<>();
        final HomeListingCards cards = new HomeListingCards(cheapest, mostRecent);
        // Exercise
        cheapest.add(new ListingCard(1L, 1L, "Ford", "Focus", new BigDecimal("80.00"), 11L));
        // Assert
        Assertions.assertSame(cheapest, cards.cheapest());
        Assertions.assertSame(mostRecent, cards.mostRecent());
        Assertions.assertEquals(1, cards.cheapest().size());
    }
}
