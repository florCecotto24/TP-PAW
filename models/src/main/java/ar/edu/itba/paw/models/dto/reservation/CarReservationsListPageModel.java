package ar.edu.itba.paw.models.dto.reservation;

import ar.edu.itba.paw.models.dto.Page;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;

/**
 * Page model consumed by {@code car/carReservations.jsp}. Built by
 * {@code ReservationViewService.loadCarReservationsListPage}.
 *
 * <p>Before this DTO the controller {@code MyCarsController.carReservations} inlined the
 * {@code getCarReservationCards + map(toReservationCardDisplayRow)} stream plus four
 * {@code mav.addObject} calls.</p>
 */
public final class CarReservationsListPageModel {

    private final Car car;
    private final List<ReservationCardDisplayRow> rows;
    private final Page<ReservationCard> resultPage;
    private final String statusFilter;

    public CarReservationsListPageModel(
            final Car car,
            final List<ReservationCardDisplayRow> rows,
            final Page<ReservationCard> resultPage,
            final String statusFilter) {
        this.car = Objects.requireNonNull(car, "car");
        this.rows = List.copyOf(rows);
        this.resultPage = Objects.requireNonNull(resultPage, "resultPage");
        this.statusFilter = statusFilter;
    }

    public Page<ReservationCard> getResultPage() { return resultPage; }

    public void populateModel(final BiConsumer<String, Object> sink) {
        sink.accept("car", car);
        sink.accept("reservations", rows);
        sink.accept("carReservationsPage", resultPage);
        sink.accept("statusFilter", statusFilter);
    }
}
