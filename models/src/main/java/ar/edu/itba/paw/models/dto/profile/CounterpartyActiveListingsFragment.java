package ar.edu.itba.paw.models.dto.profile;

import java.util.List;

import ar.edu.itba.paw.models.dto.car.CarCard;

/**
 * Result of paging through a counterparty's "other active listings" grid. Used by the AJAX
 * fragment endpoint that powers the "load more" button on {@code counterpartyProfile.jsp}.
 *
 * <p>{@link CarCard}s are exposed raw on purpose, like in {@link CounterpartyProfilePageModel}:
 * the webapp layer is the one that must convert them into its view-specific {@code
 * VehicleCardView} before rendering — the {@code models} module cannot depend on webapp types.</p>
 */
public final class CounterpartyActiveListingsFragment {

    private final List<CarCard> cards;
    private final boolean hasMore;
    private final int nextPage;

    private CounterpartyActiveListingsFragment(
            final List<CarCard> cards, final boolean hasMore, final int nextPage) {
        this.cards = List.copyOf(cards);
        this.hasMore = hasMore;
        this.nextPage = nextPage;
    }

    public static CounterpartyActiveListingsFragment of(
            final List<CarCard> cards, final boolean hasMore, final int nextPage) {
        return new CounterpartyActiveListingsFragment(cards, hasMore, nextPage);
    }

    /** Inert response when the underlying query yields no rows (counterparty not found, bad page, etc.). */
    public static CounterpartyActiveListingsFragment empty() {
        return new CounterpartyActiveListingsFragment(List.of(), false, 0);
    }

    public List<CarCard> getCards() {
        return cards;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getNextPage() {
        return nextPage;
    }
}
