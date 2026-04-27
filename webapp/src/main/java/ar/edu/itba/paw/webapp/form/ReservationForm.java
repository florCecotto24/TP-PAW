package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotNull;

import ar.edu.itba.paw.webapp.validation.constraint.ReservationWithinMaxBillableDays;

@ReservationWithinMaxBillableDays
public class ReservationForm {

    @NotNull
    private Long listingId;

    @NotNull
    private String fromDateTime;

    @NotNull
    private String untilDateTime;

    @NotNull
    private String deliveryLocation;

    @NotNull
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
