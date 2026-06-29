package ar.edu.itba.paw.webapp.form.reservation;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationAvailabilityMatchesCar;

/**
 * REST body for {@code POST /reservations} ({@code ReservationCreateDto} in openapi.yaml).
 */
@ReservationAvailabilityMatchesCar
public final class ReservationCreateForm {

    @NotBlank(message = "{validation.reservation.carUri.notBlank}")
    private String carUri;

    @NotBlank(message = "{validation.reservation.availabilityUri.notBlank}")
    private String availabilityUri;

    @NotBlank(message = "{validation.reservation.startDate.notBlank}")
    private String startDate;

    @NotBlank(message = "{validation.reservation.endDate.notBlank}")
    private String endDate;

    public String getCarUri() {
        return carUri;
    }

    public void setCarUri(final String carUri) {
        this.carUri = carUri;
    }

    public String getAvailabilityUri() {
        return availabilityUri;
    }

    public void setAvailabilityUri(final String availabilityUri) {
        this.availabilityUri = availabilityUri;
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
