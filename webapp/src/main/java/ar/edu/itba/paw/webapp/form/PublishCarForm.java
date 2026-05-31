package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.MaxFileSize;
import ar.edu.itba.paw.webapp.validation.constraint.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.support.ProfileDocumentFileSizeLimitProvider;

/** Step 1 of the two-step publish flow: vehicle attributes and pictures only. */
public final class PublishCarForm {

    /** Catalog brand ID selected by the picker; {@code 0} = "Other" (free-text). */
    private Long brandId;

    /** Catalog model ID selected by the picker; {@code 0} = "Other" (free-text). */
    private Long modelId;

    @NotBlank(message = "{validation.brand.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    @Size(message = "{validation.brand.size}", min = 2, max = 50, groups = ValidationGroups.OnPublishCar.class)
    private String brand;

    @NotBlank(message = "{validation.model.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    private String model;

    @Size(min = 6, max = 10, message = "{validation.plate.size}", groups = ValidationGroups.OnPublishCar.class)
    @NotBlank(message = "{validation.plate.notBlank}", groups = ValidationGroups.OnPublishCar.class)
    @NoPunctuation(groups = ValidationGroups.OnPublishCar.class)
    private String plate;

    /**
     * Car body type. Only required when the user creates a new {@code car_models} row (modelId == 0 /
     * "Other"); when an existing catalog model is picked, the type is read from {@link ar.edu.itba.paw.models.domain.CarModel}.
     * The conditional rule is enforced by {@link ar.edu.itba.paw.webapp.validation.PublishCarFormValidator}.
     */
    private Car.Type type;

    @NotNull(message = "{validation.powertrain.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private Car.Powertrain powertrain;

    @NotNull(message = "{validation.transmission.notNull}", groups = ValidationGroups.OnPublishCar.class)
    private Car.Transmission transmission;

    /** Optional manufacture year (>= 1886, <= current year). Upper bound checked by {@code PublishCarFormValidator}. */
    @Min(value = 1886, message = "{validation.year.min}", groups = ValidationGroups.OnPublishCar.class)
    private Integer year;

    @Size(max = 200, message = "{validation.description.size}", groups = ValidationGroups.OnPublishCar.class)
    private String description;

    /** Can be empty on retry: pictures may be in session (see {@code PublishCarFormController}). */
    @Size(max = 8, message = "{validation.pictures.size}", groups = ValidationGroups.OnPublishCar.class)
    private MultipartFile[] pictures;

    /**
     * Optional insurance document; if missing, the car is created in {@link Car.Status#LACK_DOC}.
     * {@link MaxFileSize} resolves the upper bound at runtime via the
     * {@link ProfileDocumentFileSizeLimitProvider} bean so the size cap stays sourced from
     * {@code app.upload.max-profile-document-megabytes} (no duplicated literal here).
     */
    @MaxFileSize(
            sizeProvider = ProfileDocumentFileSizeLimitProvider.class,
            message = "{car.insurance.tooLarge}",
            groups = ValidationGroups.OnPublishCar.class)
    private MultipartFile insuranceFile;

    public String getBrand() {
        return brand;
    }

    public void setBrand(final String brand) {
        this.brand = brand;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(final Long brandId) {
        this.brandId = brandId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(final String model) {
        this.model = model;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(final Long modelId) {
        this.modelId = modelId;
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

    public Integer getYear() {
        return year;
    }

    public void setYear(final Integer year) {
        this.year = year;
    }

    public MultipartFile[] getPictures() {
        return pictures;
    }

    public void setPictures(final MultipartFile[] pictures) {
        this.pictures = pictures;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public MultipartFile getInsuranceFile() {
        return insuranceFile;
    }

    public void setInsuranceFile(final MultipartFile insuranceFile) {
        this.insuranceFile = insuranceFile;
    }
}
