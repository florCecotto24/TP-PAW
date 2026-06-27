package ar.edu.itba.paw.webapp.form.car;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.user.NoPunctuation;

/** REST body for {@code POST /cars} ({@code CarCreateDto}). */
public final class CarCreateForm {

    @NotBlank(message = "{validation.plate.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    @CarValidationSize(
            kind = CarValidationSize.Kind.PLATE,
            messageKey = "validation.plate.size",
            groups = ValidationGroups.OnPublishCar.class)
    @NoPunctuation(groups = ValidationGroups.OnPublishCar.class)
    private String plate;

    private Integer year;

    @NotBlank(message = "{validation.powertrain.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private String powertrain;

    @NotBlank(message = "{validation.transmission.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private String transmission;

    @CarValidationSize(
            kind = CarValidationSize.Kind.DESCRIPTION,
            messageKey = "validation.description.size",
            groups = ValidationGroups.OnPublishCar.class)
    private String description;

    private Integer minimumRentalDays = 1;

    private String brandName;

    private String modelName;

    private String type;

    /** Alternative to brand/model when the catalog model already exists. */
    private String modelUri;

    public String getPlate() {
        return plate;
    }

    public void setPlate(final String plate) {
        this.plate = plate;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(final Integer year) {
        this.year = year;
    }

    public String getPowertrain() {
        return powertrain;
    }

    public void setPowertrain(final String powertrain) {
        this.powertrain = powertrain;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(final String transmission) {
        this.transmission = transmission;
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

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(final String brandName) {
        this.brandName = brandName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(final String modelName) {
        this.modelName = modelName;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getModelUri() {
        return modelUri;
    }

    public void setModelUri(final String modelUri) {
        this.modelUri = modelUri;
    }
}
