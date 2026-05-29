package ar.edu.itba.paw.webapp.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.CheckOutAfterCheckIn;

/** Step 2 of the two-step publish flow: listing details and per-period availability. */
@CheckOutAfterCheckIn(groups = ValidationGroups.OnCreateListing.class)
public final class CreateListingForm implements ListingTimeWindow {

    @NotNull(message = "{validation.pricePerDay.notNull}", groups = ValidationGroups.OnCreateListing.class)
    @DecimalMin(value = "0.01", message = "{validation.pricePerDay.decimalMin}", groups = ValidationGroups.OnCreateListing.class)
    @Digits(integer = 8, fraction = 2, message = "{validation.pricePerDay.digits}", groups = ValidationGroups.OnCreateListing.class)
    private BigDecimal pricePerDay;

    @NotBlank(message = "{validation.startPointStreet.notBlank}", groups = ValidationGroups.OnCreateListing.class)
    @Size(max = 250, message = "{validation.startPointStreet.size}", groups = ValidationGroups.OnCreateListing.class)
    private String startPointStreet;

    @NotBlank(message = "{validation.startPointNumber.notBlank}", groups = ValidationGroups.OnCreateListing.class)
    @Size(max = 10, message = "{validation.startPointNumber.size}", groups = ValidationGroups.OnCreateListing.class)
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
    @Size(min = 1, max = 10, message = "{validation.availabilityRows.size.range}", groups = ValidationGroups.OnCreateListing.class)
    private List<AvailabilityRow> availabilityRows = new ArrayList<>();

    @NotNull(groups = ValidationGroups.OnCreateListing.class)
    @Min(value = 1, groups = ValidationGroups.OnCreateListing.class)
    @Max(value = 365, groups = ValidationGroups.OnCreateListing.class)
    private Integer minimumRentalDays = 1;

    public CreateListingForm() {
        checkInTime = ListingAvailability.DEFAULT_CHECK_IN_TIME;
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

        @DecimalMin(value = "0.01", message = "{validation.pricePerDay.decimalMin}", groups = ValidationGroups.OnCreateListing.class)
        @Digits(integer = 8, fraction = 2, message = "{validation.pricePerDay.digits}", groups = ValidationGroups.OnCreateListing.class)
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
