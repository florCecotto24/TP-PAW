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
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.CheckOutAfterCheckIn;

@CheckOutAfterCheckIn(groups = ValidationGroups.OnListingEdit.class)
public final class ListingEditForm implements ListingTimeWindow {

    @NotNull(message = "{validation.pricePerDay.notNull}", groups = ValidationGroups.OnListingEdit.class)
    @DecimalMin(value = "0.01", message = "{validation.pricePerDay.decimalMin}", groups = ValidationGroups.OnListingEdit.class)
    @Digits(integer = 8, fraction = 2, message = "{validation.pricePerDay.digits}", groups = ValidationGroups.OnListingEdit.class)
    private BigDecimal pricePerDay;

    @NotBlank(message = "{validation.startPointStreet.notBlank}", groups = ValidationGroups.OnListingEdit.class)
    @Size(max = 250, message = "{validation.startPointStreet.size}", groups = ValidationGroups.OnListingEdit.class)
    private String startPointStreet;

    @NotBlank(message = "{validation.startPointNumber.notBlank}", groups = ValidationGroups.OnListingEdit.class)
    @Size(max = 10, message = "{validation.startPointNumber.size}", groups = ValidationGroups.OnListingEdit.class)
    @Pattern(regexp = "^[0-9]+$", message = "{validation.startPointNumber.digitsOnly}", groups = ValidationGroups.OnListingEdit.class)
    private String startPointNumber;

    @NotNull(message = "{validation.neighborhood.notNull}", groups = ValidationGroups.OnListingEdit.class)
    private Long neighborhoodId;

    @Size(max = 200, message = "{validation.description.size}", groups = ValidationGroups.OnListingEdit.class)
    private String description;

    @NotNull(message = "{validation.checkInTime.notNull}", groups = ValidationGroups.OnListingEdit.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    @NotNull(message = "{validation.checkOutTime.notNull}", groups = ValidationGroups.OnListingEdit.class)
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

    @Valid
    @Size(min = 1, max = 10,
          message = "{validation.availabilityRows.size.range}",
          groups = ValidationGroups.OnListingEdit.class)
    private List<AvailabilityRow> availabilityRows = new ArrayList<>();

    public List<AvailabilityRow> getAvailabilityRows() {
        return availabilityRows;
    }

    public void setAvailabilityRows(final List<AvailabilityRow> availabilityRows) {
        this.availabilityRows = availabilityRows;
    }

    public void populateDefaultAvailability(final List<ListingAvailability> existing) {
        if (availabilityRows.isEmpty()) {
            for (final ListingAvailability la : existing) {
                final AvailabilityRow row = new AvailabilityRow();
                row.setFrom(la.getStartInclusive());
                row.setUntil(la.getEndInclusive());
                availabilityRows.add(row);
            }
        }
    }

    public List<AvailabilityPeriod> toAvailabilityPeriods() {
        final List<AvailabilityPeriod> result = new ArrayList<>();
        for (final AvailabilityRow row : availabilityRows) {
            result.add(new AvailabilityPeriod(row.getFrom(), row.getUntil()));
        }
        return result;
    }

    public static final class AvailabilityRow {

        @NotNull(message = "{validation.availabilityRow.from.notNull}",
                 groups = ValidationGroups.OnListingEdit.class)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate from;

        @NotNull(message = "{validation.availabilityRow.until.notNull}",
                 groups = ValidationGroups.OnListingEdit.class)
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
}
