package ar.edu.itba.paw.webapp.form.car;

import ar.edu.itba.paw.webapp.validation.constraint.car.CarPatchHasField;
import ar.edu.itba.paw.webapp.validation.constraint.car.ValidCarStatus;

/** REST body for {@code PATCH /cars/{id}} ({@code CarPatchDto}). */
@CarPatchHasField
public final class CarPatchForm {

    @ValidCarStatus
    private String status;
    private String description;
    private Integer minimumRentalDays;

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Integer getMinimumRentalDays() {
        return minimumRentalDays;
    }

    public void setMinimumRentalDays(final Integer minimumRentalDays) {
        this.minimumRentalDays = minimumRentalDays;
    }
}
