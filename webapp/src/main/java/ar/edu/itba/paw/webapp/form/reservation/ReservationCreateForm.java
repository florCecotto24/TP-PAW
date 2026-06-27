package ar.edu.itba.paw.webapp.form.reservation;

import javax.validation.constraints.NotBlank;

/**
 * REST body for {@code POST /reservations} ({@code ReservationCreateDto} in openapi.yaml).
 */
public final class ReservationCreateForm {

    @NotBlank
    private String carUri;

    @NotBlank
    private String availabilityUri;

    @NotBlank
    private String startDate;

    @NotBlank
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
