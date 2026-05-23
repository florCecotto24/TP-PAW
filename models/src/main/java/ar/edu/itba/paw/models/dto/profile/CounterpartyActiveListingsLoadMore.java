package ar.edu.itba.paw.models.dto.profile;

/**
 * Paging state for the counterparty profile "other active listings" grid and its load-more control.
 */
public final class CounterpartyActiveListingsLoadMore {

    private final boolean hasNext;
    private final long ownerUserId;
    /** @deprecated Use {@link #excludeCarId} */
    @Deprecated
    private final Long excludeListingId;
    private final Long excludeCarId;
    private final int nextPageToLoad;
    private final int pageSize;

    private CounterpartyActiveListingsLoadMore(
            final boolean hasNext,
            final long ownerUserId,
            final Long excludeCarId,
            final int nextPageToLoad,
            final int pageSize) {
        this.hasNext = hasNext;
        this.ownerUserId = ownerUserId;
        this.excludeListingId = excludeCarId;
        this.excludeCarId = excludeCarId;
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
            final Long excludeCarId,
            final int nextPageToLoad,
            final int pageSize) {
        return new CounterpartyActiveListingsLoadMore(hasNext, ownerUserId, excludeCarId, nextPageToLoad, pageSize);
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    /** @deprecated Use {@link #getExcludeCarId()} */
    @Deprecated
    public Long getExcludeListingId() {
        return excludeListingId;
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
