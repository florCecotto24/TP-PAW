package ar.edu.itba.paw.models.dto.reservation;


import ar.edu.itba.paw.models.dto.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Page model consumed by {@code reservation/myReservations.jsp}. Built by
 * {@code ReservationViewService.loadRiderReservationsListPage}.
 *
 * <p>Before this DTO {@code MyReservationsController.myReservations} inlined the
 * {@code reservationService.getRiderReservationCards + stream().map(toReservationCardDisplayRow)}
 * sequence and added the same four {@code mav.addObject} calls.</p>
 */
public final class RiderReservationsListPageModel {

    private final List<ReservationCardDisplayRow> rows;
    private final Page<ReservationCard> resultPage;
    private final String currentSort;

    public RiderReservationsListPageModel(
            final List<ReservationCardDisplayRow> rows,
            final Page<ReservationCard> resultPage,
            final String currentSort) {
        this.rows = List.copyOf(rows);
        this.resultPage = Objects.requireNonNull(resultPage, "resultPage");
        this.currentSort = Objects.requireNonNull(currentSort, "currentSort");
    }

    public Page<ReservationCard> getResultPage() { return resultPage; }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("riderReservations", rows);
        sink.accept("riderReservationsPage", resultPage);
        sink.accept("activeTab", "my-reservations");
        sink.accept("currentSort", currentSort);
    }
}
