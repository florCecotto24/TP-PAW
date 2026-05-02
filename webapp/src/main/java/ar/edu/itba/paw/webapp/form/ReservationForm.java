package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotNull;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.ReservationWithinMaxBillableDays;

/** Rider reservation request: listing, wall-local pickup/return strings, handover labels, and vehicle label for display. */
@ReservationWithinMaxBillableDays(groups = ValidationGroups.OnReservationSubmit.class)
public final class ReservationForm {

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private Long listingId;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String fromDateTime;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String untilDateTime;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String deliveryLocation;

    @NotNull(groups = ValidationGroups.OnReservationSubmit.class)
    private String carName;

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
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
