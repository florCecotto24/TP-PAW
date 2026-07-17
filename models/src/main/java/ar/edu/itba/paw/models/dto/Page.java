package ar.edu.itba.paw.models.dto;

import java.util.List;

import ar.edu.itba.paw.models.pagination.UiPaging;

/** Generic paginated window: items for the current page, page index, size, and total count (see {@link UiPaging}). */
public final class Page<T> {

    private final List<T> content;
    private final int currentPage;
    private final int pageSize;
    private final long totalItems;

    public Page(final List<T> content, final int currentPage, final int pageSize, final long totalItems) {
        this.content = content == null ? List.of() : List.copyOf(content);
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    /** Unmodifiable snapshot of the current page items (defensive copy taken at construction). */
    public List<T> getContent() {
        return content;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return UiPaging.totalPages(totalItems, pageSize);
    }

    public boolean isHasPrevious() {
        return currentPage > 0;
    }

    public boolean isHasNext() {
        return currentPage < getTotalPages() - 1;
    }

    public int getFirstItemNumber() {
        return currentPage * pageSize + 1;
    }

    public long getLastItemNumber() {
        return Math.min((long) (currentPage + 1) * pageSize, totalItems);
    }
}
