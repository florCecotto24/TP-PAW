package ar.edu.itba.paw.webapp.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.webapp.validation.CheckOutAfterCheckIn;

@CheckOutAfterCheckIn
public class PublishCarForm {

    @NotBlank(message = "{validation.brand.notBlank}")
    @Size(message = "{validation.brand.size}", min = 2, max = 50)
    private String brand;

    @NotBlank(message = "{validation.model.notBlank}")
    private String model;

    @Size(min = 6, max = 10, message = "Plate must be between 6 and 10 characters")
    @NotBlank(message = "{validation.plate.notBlank}")
    // Acá podríamos poner las validaciones de las patentes argentinas (dividir entre nuevas y viejas)
    private String plate;

    @NotNull(message = "{validation.type.notNull}")
    private Car.Type type;

    @NotNull(message = "{validation.powertrain.notNull}")
    private Car.Powertrain powertrain;

    @NotNull(message = "{validation.transmission.notNull}")
    private Car.Transmission transmission;

    @NotNull(message = "{validation.pricePerDay.notNull}")
    @DecimalMin(value = "0.01", message = "{validation.pricePerDay.decimalMin}")
    @Digits(integer = 8, fraction = 2, message = "{validation.pricePerDay.digits}")
    private BigDecimal pricePerDay;

    @Size(max = 250, message = "Description must be at most 250 characters")
    @NotBlank(message = "{validation.startPoint.notBlank}")
    private String startPoint;

    @Size(max = 200, message = "{validation.description.size}")
    private String description;

    @NotNull(message = "{validation.checkInTime.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    @NotNull(message = "{validation.checkOutTime.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkOutTime;

    /**
     * Can be empty in a retry: the pictures may be in session (see {@code PublishCarFormController}).
     */
    @Size(max = 8, message = "{validation.pictures.size}")
    private MultipartFile[] pictures;

    @Size(min = 1, max = 10, message = "Add between 1 and 10 availability periods")
    @Size(max = 10, message = "{validation.availabilityRows.size}")
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
