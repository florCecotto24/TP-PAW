package ar.edu.itba.paw.models.dto.car;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.dto.Page;

/**
 * Page model consumed by {@code car/myCars.jsp}. Before this DTO existed,
 * {@code MyCarsController.myCars} stitched the owner-cars search results, the per-card
 * refund-pending badge ids, and the "current user blocked" flag together by calling
 * {@code CarService} and {@code ReservationService} side by side from the GET handler.
 * Folding everything into a single page model keeps the controller down to a single
 * view-service call.
 */
public final class MyCarsListPageModel {

    private final Page<CarCard> resultPage;
    private final String listingsCurrentSort;
    private final boolean currentUserBlocked;
    private final Set<Long> pendingRefundCarIds;

    public MyCarsListPageModel(
            final Page<CarCard> resultPage,
            final String listingsCurrentSort,
            final boolean currentUserBlocked,
            final Set<Long> pendingRefundCarIds) {
        this.resultPage = Objects.requireNonNull(resultPage, "resultPage");
        this.listingsCurrentSort = Objects.requireNonNull(listingsCurrentSort, "listingsCurrentSort");
        this.currentUserBlocked = currentUserBlocked;
        this.pendingRefundCarIds = pendingRefundCarIds == null ? Set.of() : Set.copyOf(pendingRefundCarIds);
    }

    public Page<CarCard> getResultPage() { return resultPage; }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("results", resultPage.getContent());
        sink.accept("myListingsPage", resultPage);
        sink.accept("listingsCurrentSort", listingsCurrentSort);
        sink.accept("currentUserBlocked", currentUserBlocked);
        // Per-car badge "you have a reservation requiring a refund receipt": independent of whether
        // the owner is already blocked, so it surfaces the obligation even before the deadline expires.
        sink.accept("pendingRefundCarIds", pendingRefundCarIds);
    }
}
