package ar.edu.itba.paw.webapp.form.car;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingAvailabilityRowsSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingFormValidationSize.Kind;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingFormValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingMinimumRentalDays;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingPricePerDay;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.CheckOutAfterCheckIn;

/** Step 2 of the two-step publish flow: listing details and per-period availability. */
@CheckOutAfterCheckIn(groups = ValidationGroups.OnCreateListing.class)
public final class CreateCarAvailabilityForm implements CarAvailabilityTimeWindow {

    @NotNull(message = "{validation.pricePerDay.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @ListingPricePerDay(groups = ValidationGroups.OnCreateListing.class)
    private BigDecimal pricePerDay;

    @NotBlank(message = "{validation.startPointStreet.notBlank}", groups = ValidationGroups.OnCreateListing.class)
    @ListingFormValidationSize(
            kind = Kind.ADDRESS_STREET,
            messageKey = "validation.startPointStreet.size",
            groups = ValidationGroups.OnCreateListing.class)
    private String startPointStreet;

    @NotBlank(message = "{validation.startPointNumber.notBlank}", groups = ValidationGroups.OnCreateListing.class)
    @ListingFormValidationSize(
            kind = Kind.ADDRESS_NUMBER,
            messageKey = "validation.startPointNumber.size",
            groups = ValidationGroups.OnCreateListing.class)
    @Pattern(regexp = "^[0-9]+$", message = "{validation.startPointNumber.digitsOnly}", groups = ValidationGroups.OnCreateListing.class)
    private String startPointNumber;

    @NotNull(message = "{validation.neighborhood.notNull}", groups = ValidationGroups.OnCreateListing.class)
    private Long neighborhoodId;

    @NotNull(message = "{validation.checkInTime.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    @NotNull(message = "{validation.checkOutTime.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkOutTime;

    @Valid
    @ListingAvailabilityRowsSize(
            enforceMinimum = true,
            messageKey = "validation.availabilityRows.size.range",
            groups = ValidationGroups.OnCreateListing.class)
    private List<AvailabilityRow> availabilityRows = new ArrayList<>();

    @NotNull(groups = ValidationGroups.OnCreateListing.class)
    @ListingMinimumRentalDays(groups = ValidationGroups.OnCreateListing.class)
    private Integer minimumRentalDays = 1;

    public CreateCarAvailabilityForm() {
        checkInTime = CarAvailability.DEFAULT_CHECK_IN_TIME;
        checkOutTime = CarAvailability.DEFAULT_CHECK_OUT_TIME;
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

    public List<BigDecimal> toPeriodPrices() {
        final List<BigDecimal> prices = new ArrayList<>();
        for (final AvailabilityRow row : availabilityRows) {
            if (row.getFrom() != null && row.getUntil() != null) {
                prices.add(row.getDayPrice());
            }
        }
        return prices;
    }

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

    public List<AvailabilityRow> getAvailabilityRows() {
        return availabilityRows;
    }

    public void setAvailabilityRows(final List<AvailabilityRow> availabilityRows) {
        this.availabilityRows = availabilityRows;
    }

    public Integer getMinimumRentalDays() {
        return minimumRentalDays;
    }

    public void setMinimumRentalDays(final Integer minimumRentalDays) {
        this.minimumRentalDays = minimumRentalDays;
    }

    public static final class AvailabilityRow {

        @NotNull(message = "{validation.availabilityRow.from.notNull}", groups = ValidationGroups.OnCreateListing.class)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate from;

        @NotNull(message = "{validation.availabilityRow.until.notNull}", groups = ValidationGroups.OnCreateListing.class)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate until;

        @ListingPricePerDay(groups = ValidationGroups.OnCreateListing.class)
        private BigDecimal dayPrice;

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

        public BigDecimal getDayPrice() {
            return dayPrice;
        }

        public void setDayPrice(final BigDecimal dayPrice) {
            this.dayPrice = dayPrice;
        }
    }
}
