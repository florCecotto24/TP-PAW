package ar.edu.itba.paw.models;

import java.util.List;

public final class Page<T> {

    private final List<T> content;
    private final int currentPage;
    private final int pageSize;
    private final long totalItems;

    public Page(final List<T> content, final int currentPage, final int pageSize, final long totalItems) {
        this.content = content;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

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
        return totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
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
