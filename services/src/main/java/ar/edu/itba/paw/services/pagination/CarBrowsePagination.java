package ar.edu.itba.paw.services.pagination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.pagination.DualLayerPageWindow;
import ar.edu.itba.paw.models.pagination.SingleLayerPageWindow;
import ar.edu.itba.paw.services.policy.PaginationPolicy;

/**
 * Resolves listing pagination windows from {@link PaginationPolicy} so services/controllers avoid ad-hoc math.
 */
@Component
public final class CarBrowsePagination {

    private final PaginationPolicy paginationPolicy;

    @Autowired
    public CarBrowsePagination(final PaginationPolicy paginationPolicy) {
        this.paginationPolicy = paginationPolicy;
    }

    /**
     * @param uiPage              0-based UI page index
     * @param uiPageSizeOrZero    if ≤ 0, {@link PaginationPolicy#getUiPageSize()} is used
     */
    public DualLayerPageWindow window(final int uiPage, final int uiPageSizeOrZero) {
        final int ui = uiPageSizeOrZero > 0 ? uiPageSizeOrZero : paginationPolicy.getUiPageSize();
        return DualLayerPageWindow.compute(uiPage, ui, paginationPolicy.getDbFetchSize());
    }

    /**
     * Single-layer grid (same size for SQL {@code LIMIT} and UI), e.g. owner listing cards.
     *
     * @param page               0-based page index
     * @param pageSizeOrZero     if ≤ 0, {@link PaginationPolicy#getDefaultPageSize()} is used
     */
    public SingleLayerPageWindow gridWindow(final int page, final int pageSizeOrZero) {
        final int ps = pageSizeOrZero > 0 ? pageSizeOrZero : paginationPolicy.getDefaultPageSize();
        return SingleLayerPageWindow.compute(page, ps);
    }
}
