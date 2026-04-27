package ar.edu.itba.paw.webapp.form;

import java.math.BigDecimal;
import java.time.LocalTime;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.webapp.validation.constraint.CheckOutAfterCheckIn;

@CheckOutAfterCheckIn
public class ListingEditForm implements ListingTimeWindow {

    @NotNull(message = "{validation.pricePerDay.notNull}")
    @DecimalMin(value = "0.01", message = "{validation.pricePerDay.decimalMin}")
    @Digits(integer = 8, fraction = 2, message = "{validation.pricePerDay.digits}")
    private BigDecimal pricePerDay;

    @NotBlank(message = "{validation.startPointStreet.notBlank}")
    @Size(max = 250, message = "{validation.startPointStreet.size}")
    private String startPointStreet;

    @NotBlank(message = "{validation.startPointNumber.notBlank}")
    @Size(max = 10, message = "{validation.startPointNumber.size}")
    @Pattern(regexp = "^[0-9]+$", message = "{validation.startPointNumber.digitsOnly}")
    private String startPointNumber;

    @NotNull(message = "{validation.neighborhood.notNull}")
    private Long neighborhoodId;

    @Size(max = 200, message = "{validation.description.size}")
    private String description;

    @NotNull(message = "{validation.checkInTime.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    @NotNull(message = "{validation.checkOutTime.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkOutTime;


    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(final BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getStartPointStreet() {
        return startPointStreet;
    }

    public void setStartPointStreet(final String startPointStreet) {
        this.startPointStreet = startPointStreet;
    }

    public String getStartPointNumber() {
        return startPointNumber;
    }

    public void setStartPointNumber(final String startPointNumber) {
        this.startPointNumber = startPointNumber;
    }

    public Long getNeighborhoodId() {
        return neighborhoodId;
    }

    public void setNeighborhoodId(final Long neighborhoodId) {
        this.neighborhoodId = neighborhoodId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(final LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    @Override
    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(final LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
