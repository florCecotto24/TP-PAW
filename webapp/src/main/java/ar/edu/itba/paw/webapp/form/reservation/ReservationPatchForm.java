package ar.edu.itba.paw.webapp.form.reservation;

import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationPatchDatesTogether;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationPatchHasField;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.ValidReservationPatchStatus;

/**
 * REST body for {@code PATCH /reservations/{id}} ({@code ReservationPatchDto} in openapi.yaml).
 */
@ReservationPatchHasField
@ReservationPatchDatesTogether
public final class ReservationPatchForm {

    @ValidReservationPatchStatus
    private String status;
    private Boolean carReturned;
    private String startDate;
    private String endDate;

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Boolean getCarReturned() {
        return carReturned;
    }

    public void setCarReturned(final Boolean carReturned) {
        this.carReturned = carReturned;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(final String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(final String endDate) {
        this.endDate = endDate;
    }
}
