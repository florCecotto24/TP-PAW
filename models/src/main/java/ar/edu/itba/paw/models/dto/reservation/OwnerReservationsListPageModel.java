package ar.edu.itba.paw.models.dto.reservation;

import ar.edu.itba.paw.models.dto.Page;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.car.Car;

/**
 * Page model consumed by {@code reservation/ownerReservations.jsp}. Used by both the
 * unscoped {@code /my-cars/reservations} handler and the per-car
 * {@code /my-cars/reservations/{carId}} variant: the only difference is whether
 * {@link #getSelectedCar()} is present.
 *
 * <p>Before this DTO {@code MyCarsController.ownerReservations} and
 * {@code MyCarsController.ownerReservationsForCar} duplicated the
 * {@code reservationService.getOwnerReservationCards + map(toReservationCardDisplayRow)} stream
 * inline and added the same five {@code mav.addObject} calls.</p>
 */
public final class OwnerReservationsListPageModel {

    private final List<ReservationCardDisplayRow> rows;
    private final Page<ReservationCard> resultPage;
    private final Car selectedCarOrNull;
    private final String currentSort;
    private final Set<Long> pendingRefundReservationIds;

    public OwnerReservationsListPageModel(
            final List<ReservationCardDisplayRow> rows,
            final Page<ReservationCard> resultPage,
            final Car selectedCarOrNull,
            final String currentSort,
            final Set<Long> pendingRefundReservationIds) {
        this.rows = List.copyOf(rows);
        this.resultPage = Objects.requireNonNull(resultPage, "resultPage");
        this.selectedCarOrNull = selectedCarOrNull;
        this.currentSort = Objects.requireNonNull(currentSort, "currentSort");
        this.pendingRefundReservationIds = pendingRefundReservationIds == null
                ? Set.of()
                : Set.copyOf(pendingRefundReservationIds);
    }

    public Page<ReservationCard> getResultPage() { return resultPage; }

    public Optional<Car> getSelectedCar() { return Optional.ofNullable(selectedCarOrNull); }

    public Set<Long> getPendingRefundReservationIds() { return pendingRefundReservationIds; }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("ownerReservations", rows);
        sink.accept("ownerReservationsPage", resultPage);
        sink.accept("selectedCar", selectedCarOrNull);
        sink.accept("ownerCurrentSort", currentSort);
        // Per-reservation badge "you must upload a refund receipt for this reservation": ids
        // indexed by reservation so the cards on this page can render a flag without an extra
        // controller-side lookup.
        sink.accept("pendingRefundReservationIds", pendingRefundReservationIds);
    }
}
