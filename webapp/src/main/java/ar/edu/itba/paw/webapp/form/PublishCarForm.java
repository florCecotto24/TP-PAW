package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class PublishCarForm {

    @NotBlank(message = "Your name is required")
    @Size(max = 50, message = "Name must be at most 50 characters")
    private String ownerName;

    @NotBlank(message = "Your surname is required")
    @Size(max = 50, message = "Surname must be at most 50 characters")
    private String ownerSurname;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 50, message = "Email must be at most 50 characters")
    private String ownerEmail;

    @NotBlank(message = "Brand is required")
    @Size(message = "Brand muste be between 2 and 50 characters", min = 2, max = 50)
    private String brand;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Plate is required")
    @Size(min = 6, max = 10, message = "Plate must be between 6 and 10 characters")
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
    @Digits(integer = 10, fraction = 2, message = "Price must have up to 2 decimals")
    private BigDecimal pricePerDay;

    @NotBlank(message = "Start point is required")
    @Size(max = 250, message = "Description must be at most 250 characters")
    private String startPoint;

    @Size(max = 200, message = "Description must be at most 200 characters")
    private String description;

    @NotNull(message = "Check-in time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    @NotNull(message = "Check-out time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkOutTime;

    @NotNull(message = "At least one image is required")
    @Size(min = 1, max = 8, message = "Upload between 1 and 8 images")
    private MultipartFile[] pictures;

    @Size(min = 1, max = 10, message = "Add between 1 and 10 availability periods")
    private List<@Valid AvailabilityRow> availabilityRows = new ArrayList<>();

    public PublishCarForm() {
        checkInTime = LocalTime.of(10, 0);
        checkOutTime = LocalTime.of(18, 0);
        availabilityRows.add(new AvailabilityRow());
    }

    public List<AvailabilityPeriod> toAvailabilityPeriods() {
        final List<AvailabilityPeriod> periods = new ArrayList<>();
        for (final AvailabilityRow row : availabilityRows) {
            if (row.getFrom() != null && row.getUntil() != null) {
                periods.add(new AvailabilityPeriod(row.getFrom(), row.getUntil()));
            }
        }
        return periods;
    }

    public static class AvailabilityRow {
        @NotNull(message = "Start date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate from;

        @NotNull(message = "End date is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate until;

        public LocalDate getFrom() {
            return from;
        }

        public void setFrom(final LocalDate from) {
            this.from = from;
        }

        public LocalDate getUntil() {
            return until;
        }

        public void setUntil(final LocalDate until) {
            this.until = until;
        }
    }

    public MultipartFile[] getPictures() {
        return pictures;
    }

    public void setPictures(final MultipartFile[] pictures) {
        this.pictures = pictures;
    }

    public List<AvailabilityRow> getAvailabilityRows() {
        return availabilityRows;
    }

    public void setAvailabilityRows(final List<AvailabilityRow> availabilityRows) {
        this.availabilityRows = availabilityRows;
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

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(final String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerSurname() {
        return ownerSurname;
    }

    public void setOwnerSurname(final String ownerSurname) {
        this.ownerSurname = ownerSurname;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(final String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(final LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(final LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
