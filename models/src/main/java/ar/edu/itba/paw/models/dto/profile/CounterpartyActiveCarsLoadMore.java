package ar.edu.itba.paw.models.dto.profile;

/**
 * Paging state for the counterparty profile "other active listings" grid and its load-more control.
 */
public final class CounterpartyActiveCarsLoadMore {

    private final boolean hasNext;
    private final long ownerUserId;
    private final Long excludeCarId;
    private final int nextPageToLoad;
    private final int pageSize;

    private CounterpartyActiveCarsLoadMore(
            final boolean hasNext,
            final long ownerUserId,
            final Long excludeCarId,
            final int nextPageToLoad,
            final int pageSize) {
        this.hasNext = hasNext;
        this.ownerUserId = ownerUserId;
        this.excludeCarId = excludeCarId;
        this.nextPageToLoad = nextPageToLoad;
        this.pageSize = pageSize;
    }

    /** Inert state when the grid or button is not shown. */
    public static CounterpartyActiveCarsLoadMore none() {
        return new CounterpartyActiveCarsLoadMore(false, 0L, null, 0, 0);
    }

    public static CounterpartyActiveCarsLoadMore of(
            final boolean hasNext,
            final long ownerUserId,
            final Long excludeCarId,
            final int nextPageToLoad,
            final int pageSize) {
        return new CounterpartyActiveCarsLoadMore(hasNext, ownerUserId, excludeCarId, nextPageToLoad, pageSize);
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    public Long getExcludeCarId() {
        return excludeCarId;
    }

    public int getNextPageToLoad() {
        return nextPageToLoad;
    }

    public int getPageSize() {
        return pageSize;
    }
}
