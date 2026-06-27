package ar.edu.itba.paw.webapp.form.car;

import javax.validation.constraints.NotNull;

import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingMinimumRentalDays;

/** REST body for {@code PUT /cars/{id}} (owner replaces editable car attributes). */
public final class CarReplaceForm {

    @CarValidationSize(kind = CarValidationSize.Kind.DESCRIPTION, messageKey = "validation.description.size")
    private String description;

    @NotNull
    @ListingMinimumRentalDays
    private Integer minimumRentalDays;

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
