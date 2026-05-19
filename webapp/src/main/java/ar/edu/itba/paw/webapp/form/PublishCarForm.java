package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.NoPunctuation;

/** Step 1 of the two-step publish flow: vehicle attributes and pictures only. */
public final class PublishCarForm {

    @NotBlank(message = "{validation.brand.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    @Size(message = "{validation.brand.size}", min = 2, max = 50, groups = ValidationGroups.OnPublishCar.class)
    private String brand;

    @NotBlank(message = "{validation.model.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    private String model;

    @Size(min = 6, max = 10, message = "{validation.plate.size}", groups = ValidationGroups.OnPublishCar.class)
    @NotBlank(message = "{validation.plate.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    @NoPunctuation(groups = ValidationGroups.OnPublishCar.class)
    private String plate;

    @NotNull(message = "{validation.type.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private Car.Type type;

    @NotNull(message = "{validation.powertrain.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private Car.Powertrain powertrain;

    @NotNull(message = "{validation.transmission.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private Car.Transmission transmission;

    /** Can be empty on retry: pictures may be in session (see {@code PublishCarFormController}). */
    @Size(max = 8, message = "{validation.pictures.size}", groups = ValidationGroups.OnPublishCar.class)
    private MultipartFile[] pictures;

    public String getBrand() {
        return brand;
    }

    public void setBrand(final String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(final String plate) {
        this.plate = plate;
    }

    public Car.Type getType() {
        return type;
    }

    public void setType(final Car.Type type) {
        this.type = type;
    }

    public Car.Powertrain getPowertrain() {
        return powertrain;
    }

    public void setPowertrain(final Car.Powertrain powertrain) {
        this.powertrain = powertrain;
    }

    public Car.Transmission getTransmission() {
        return transmission;
    }

    public void setTransmission(final Car.Transmission transmission) {
        this.transmission = transmission;
    }

    public MultipartFile[] getPictures() {
        return pictures;
    }

    public void setPictures(final MultipartFile[] pictures) {
        this.pictures = pictures;
    }
}
