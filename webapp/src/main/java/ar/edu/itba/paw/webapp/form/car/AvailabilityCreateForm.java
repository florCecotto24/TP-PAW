package ar.edu.itba.paw.webapp.form.car;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.car.EndDateOnOrAfterStartDate;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingFormValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingPricePerDay;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.CheckOutAfterCheckIn;

/** REST body for availability create/edit ({@code AvailabilityCreateDto}). */
@CheckOutAfterCheckIn(groups = ValidationGroups.OnCreateListing.class)
@EndDateOnOrAfterStartDate(groups = ValidationGroups.OnCreateListing.class)
public final class AvailabilityCreateForm implements CarAvailabilityTimeWindow {

    @NotNull(groups = ValidationGroups.OnCreateListing.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(groups = ValidationGroups.OnCreateListing.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotNull(message = "{validation.pricePerDay.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @ListingPricePerDay(groups = ValidationGroups.OnCreateListing.class)
    private BigDecimal dayPrice;

    @NotBlank(message = "{validation.startPointStreet.notBlank}", groups = ValidationGroups.OnCreateListing.class)
    @ListingFormValidationSize(
            kind = ListingFormValidationSize.Kind.ADDRESS_STREET,
            messageKey = "validation.startPointStreet.size",
            groups = ValidationGroups.OnCreateListing.class)
    private String startPointStreet;

    @ListingFormValidationSize(
            kind = ListingFormValidationSize.Kind.ADDRESS_NUMBER,
            messageKey = "validation.startPointNumber.size",
            groups = ValidationGroups.OnCreateListing.class)
    @Pattern(regexp = "^[0-9]*$", message = "{validation.startPointNumber.digitsOnly}", groups = ValidationGroups.OnCreateListing.class)
    private String startPointNumber;

    @NotNull(message = "{validation.neighborhood.notNull}", groups = ValidationGroups.OnCreateListing.class)
    private Long neighborhoodId;

    @NotNull(message = "{validation.checkInTime.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime = CarAvailability.DEFAULT_CHECK_IN_TIME;

    @NotNull(message = "{validation.checkOutTime.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkOutTime = CarAvailability.DEFAULT_CHECK_OUT_TIME;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public void setDayPrice(final BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
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
