package ar.edu.itba.paw.models.dto.reservation;

import java.util.function.BiConsumer;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Reservation;

/**
 * Model attributes for the rider-side {@code myReservationEdit} JSP: identifies the reservation being
 * edited, exposes the car summary used for breadcrumbs and labels, the inline-calendar bookable
 * segments (excluding the reservation itself so its current days stay selectable) and the maximum
 * billable days policy.
 *
 * Built by {@code ReservationViewService} so the controller stays free of domain reads.
 */
public final class ReservationEditPageModel {

    private final Reservation reservation;
    private final Car car;
    private final long carImageId;
    private final String currentPickupDateTimeDisplay;
    private final String currentReturnDateTimeDisplay;
    private final String currentTotalPriceDisplay;
    /** Pre-fill values for the hidden Flatpickr inputs (wall-zone {@code yyyy-MM-dd'T'HH:mm}). */
    private final String currentFromDateTimeWall;
    private final String currentUntilDateTimeWall;
    private final String bookableWallRangesJson;
    private final boolean hasBookableDays;
    private final int maxReservationBillableDays;
    private final int minimumRentalDays;

    public ReservationEditPageModel(
            final Reservation reservation,
            final Car car,
            final long carImageId,
            final String currentPickupDateTimeDisplay,
            final String currentReturnDateTimeDisplay,
            final String currentTotalPriceDisplay,
            final String currentFromDateTimeWall,
            final String currentUntilDateTimeWall,
            final String bookableWallRangesJson,
            final boolean hasBookableDays,
            final int maxReservationBillableDays,
            final int minimumRentalDays) {
        this.reservation = reservation;
        this.car = car;
        this.carImageId = carImageId;
        this.currentPickupDateTimeDisplay = currentPickupDateTimeDisplay;
        this.currentReturnDateTimeDisplay = currentReturnDateTimeDisplay;
        this.currentTotalPriceDisplay = currentTotalPriceDisplay;
        this.currentFromDateTimeWall = currentFromDateTimeWall;
        this.currentUntilDateTimeWall = currentUntilDateTimeWall;
        this.bookableWallRangesJson = bookableWallRangesJson;
        this.hasBookableDays = hasBookableDays;
        this.maxReservationBillableDays = maxReservationBillableDays;
        this.minimumRentalDays = minimumRentalDays;
    }

    public long getReservationId() {
        return reservation.getId();
    }

    public long getCarId() {
        return car.getId();
    }

    public final void populateModel(final BiConsumer<String, Object> putObject) {
        putObject.accept("reservation", reservation);
        putObject.accept("car", car);
        putObject.accept("carImageId", carImageId);
        putObject.accept("currentPickupDateTimeDisplay", currentPickupDateTimeDisplay);
        putObject.accept("currentReturnDateTimeDisplay", currentReturnDateTimeDisplay);
        putObject.accept("currentTotalPriceDisplay", currentTotalPriceDisplay);
        putObject.accept("currentFromDateTimeWall", currentFromDateTimeWall);
        putObject.accept("currentUntilDateTimeWall", currentUntilDateTimeWall);
        putObject.accept("bookableWallRangesJson", bookableWallRangesJson);
        putObject.accept("hasBookableDays", hasBookableDays);
        putObject.accept("maxReservationBillableDays", maxReservationBillableDays);
        putObject.accept("minimumRentalDays", minimumRentalDays);
    }
}
