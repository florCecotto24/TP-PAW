package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.ReservationWithinMaxBillableDays;

/** Rider reservation request: car, wall-local pickup/return strings, handover labels, and vehicle label for display. */
@ReservationWithinMaxBillableDays(groups = ValidationGroups.OnReservationSubmit.class)
public final class ReservationForm {

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private Long carId;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String fromDateTime;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String untilDateTime;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String deliveryLocation;

    @NotBlank(
            message = "{reservation.form.carNameRequired}",
            groups = ValidationGroups.OnReservationSubmit.class)
    private String carName;

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public String getFromDateTime() {
        return fromDateTime;
    }

    public void setFromDateTime(String fromDateTime) {
        this.fromDateTime = fromDateTime;
    }

    public String getUntilDateTime() {
        return untilDateTime;
    }

    public void setUntilDateTime(String untilDateTime) {
        this.untilDateTime = untilDateTime;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getCarName() {
        return carName;
    }

    public void setCarName(String carName) {
        this.carName = carName;
    }
}
