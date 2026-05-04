package ar.edu.itba.paw.models.dto.profile;

/**
 * Paging state for the counterparty profile “other active listings” grid and its load-more control.
 */
public final class CounterpartyActiveListingsLoadMore {

    private final boolean hasNext;
    private final long ownerUserId;
    private final Long excludeListingId;
    private final int nextPageToLoad;
    private final int pageSize;

    private CounterpartyActiveListingsLoadMore(
            final boolean hasNext,
            final long ownerUserId,
            final Long excludeListingId,
            final int nextPageToLoad,
            final int pageSize) {
        this.hasNext = hasNext;
        this.ownerUserId = ownerUserId;
        this.excludeListingId = excludeListingId;
        this.nextPageToLoad = nextPageToLoad;
        this.pageSize = pageSize;
    }

    /** Inert state when the grid or button is not shown. */
    public static CounterpartyActiveListingsLoadMore none() {
        return new CounterpartyActiveListingsLoadMore(false, 0L, null, 0, 0);
    }

    public static CounterpartyActiveListingsLoadMore of(
            final boolean hasNext,
            final long ownerUserId,
            final Long excludeListingId,
            final int nextPageToLoad,
            final int pageSize) {
        return new CounterpartyActiveListingsLoadMore(hasNext, ownerUserId, excludeListingId, nextPageToLoad, pageSize);
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    public Long getExcludeListingId() {
        return excludeListingId;
    }

    public int getNextPageToLoad() {
        return nextPageToLoad;
    }

    public int getPageSize() {
        return pageSize;
    }
}
