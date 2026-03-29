package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.Car;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;
import java.math.BigDecimal;

public class PublishCarForm {

    @NotBlank(message = "Brand is required")
    @Size(message = "Brand muste be between 2 and 50 characters", min = 2, max = 50)
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Plate is required")
    // Acá podríamos poner las validaciones de las patentes argentinas (dividir entre nuevas y viejas)
    private String plate;

    @NotNull(message = "Type is required")
    private Car.Type type;

    @NotNull(message = "Powertrain is required")
    private Car.Powertrain powertrain;

    @NotNull(message = "Transmission is required")
    private Car.Transmission transmission;

    @NotNull(message = "Price per day is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have up to 2 decimals")
    private BigDecimal pricePerDay;

    @NotBlank(message = "Start point is required")
    private String startPoint;

    @Size(max = 200, message = "Description must be at most 200 characters")
    private String description;

    @NotNull(message = "At least one image is required")
    @Size(min = 1, max = 8, message = "Upload between 1 and 8 images")
    private MultipartFile[] pictures;

    public MultipartFile[] getPictures() {
        return pictures;
    }

    public void setPictures(MultipartFile[] pictures) {
        this.pictures = pictures;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public Car.Type getType() {
        return type;
    }

    public void setType(Car.Type type) {
        this.type = type;
    }

    public Car.Powertrain getPowertrain() {
        return powertrain;
    }

    public void setPowertrain(Car.Powertrain powertrain) {
        this.powertrain = powertrain;
    }

    public Car.Transmission getTransmission() {
        return transmission;
    }

    public void setTransmission(Car.Transmission transmission) {
        this.transmission = transmission;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(String startPoint) {
        this.startPoint = startPoint;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
