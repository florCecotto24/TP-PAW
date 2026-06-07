package ar.edu.itba.paw.models.dto.reservation;

import ar.edu.itba.paw.models.dto.Page;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

    public OwnerReservationsListPageModel(
            final List<ReservationCardDisplayRow> rows,
            final Page<ReservationCard> resultPage,
            final Car selectedCarOrNull,
            final String currentSort) {
        this.rows = List.copyOf(rows);
        this.resultPage = Objects.requireNonNull(resultPage, "resultPage");
        this.selectedCarOrNull = selectedCarOrNull;
        this.currentSort = Objects.requireNonNull(currentSort, "currentSort");
    }

    public Page<ReservationCard> getResultPage() { return resultPage; }

    public Optional<Car> getSelectedCar() { return Optional.ofNullable(selectedCarOrNull); }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("ownerReservations", rows);
        sink.accept("ownerReservationsPage", resultPage);
        sink.accept("selectedCar", selectedCarOrNull);
        sink.accept("ownerCurrentSort", currentSort);
    }
}
