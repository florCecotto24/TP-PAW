package ar.edu.itba.paw.webapp.form.car;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.car.CarValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingAvailabilityRowsSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingFormValidationSize;
import ar.edu.itba.paw.webapp.validation.constraint.car.ListingPricePerDay;
import ar.edu.itba.paw.webapp.validation.constraint.reservation.CheckOutAfterCheckIn;

/** Owner listing edit: price, handover address, availability periods, and neighborhood. */
@CheckOutAfterCheckIn(groups = ValidationGroups.OnListingEdit.class)
public final class CarAvailabilityEditForm implements CarAvailabilityTimeWindow {

    @NotNull(message = "{validation.pricePerDay.notNull}", groups = ValidationGroups.OnListingEdit.class)
    @ListingPricePerDay(groups = ValidationGroups.OnListingEdit.class)
    private BigDecimal pricePerDay;

    @NotBlank(message = "{validation.startPointStreet.notBlank}", groups = ValidationGroups.OnListingEdit.class)
    @ListingFormValidationSize(
            kind = ListingFormValidationSize.Kind.ADDRESS_STREET,
            messageKey = "validation.startPointStreet.size",
            groups = ValidationGroups.OnListingEdit.class)
    private String startPointStreet;

    @NotBlank(message = "{validation.startPointNumber.notBlank}", groups = ValidationGroups.OnListingEdit.class)
    @ListingFormValidationSize(
            kind = ListingFormValidationSize.Kind.ADDRESS_NUMBER,
            messageKey = "validation.startPointNumber.size",
            groups = ValidationGroups.OnListingEdit.class)
    @Pattern(regexp = "^[0-9]+$", message = "{validation.startPointNumber.digitsOnly}", groups = ValidationGroups.OnListingEdit.class)
    private String startPointNumber;

    @NotNull(message = "{validation.neighborhood.notNull}", groups = ValidationGroups.OnListingEdit.class)
    private Long neighborhoodId;

    @CarValidationSize(
            kind = CarValidationSize.Kind.DESCRIPTION,
            messageKey = "validation.description.size",
            groups = ValidationGroups.OnListingEdit.class)
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
    @ListingAvailabilityRowsSize(
            enforceMinimum = false,
            messageKey = "validation.availabilityRows.size.range",
            groups = ValidationGroups.OnListingEdit.class)
    private List<AvailabilityRow> availabilityRows = new ArrayList<>();

    public List<AvailabilityRow> getAvailabilityRows() {
        return availabilityRows;
    }

    public void setAvailabilityRows(final List<AvailabilityRow> availabilityRows) {
        this.availabilityRows = availabilityRows;
    }

    public void populateDefaultAvailability(final List<CarAvailability> existing) {
        if (availabilityRows.isEmpty()) {
            final LocalDate today = LocalDate.now(AppTimezone.WALL_ZONE);
            for (final CarAvailability la : existing) {
                if (!la.getEndInclusive().isBefore(today)) {
                    final AvailabilityRow row = new AvailabilityRow();
                    row.setFrom(la.getStartInclusive());
                    row.setUntil(la.getEndInclusive());
                    row.setDayPrice(la.getDayPrice().orElse(null));
                    availabilityRows.add(row);
                }
            }
        }
    }

    /**
     * Populates the unset header fields of this edit form (price, address, check-in/out times,
     * neighborhood) from the most recently created availability of {@code existing}, then
     * appends the still-active availabilities as default rows via
     * {@link #populateDefaultAvailability(List)}. Pre-existing form values are preserved so
     * binding errors that re-render the form keep what the user typed.
     */
    public void populateDefaultsFromExistingAvailabilities(final List<CarAvailability> existing) {
        if (existing == null || existing.isEmpty()) {
            return;
        }
        final CarAvailability mostRecent = existing.stream()
                .max(Comparator.comparing(CarAvailability::getCreatedAt))
                .get();
        if (pricePerDay == null) {
            pricePerDay = mostRecent.getDayPriceValue();
        }
        if (startPointStreet == null) {
            startPointStreet = mostRecent.getStartPointStreet();
        }
        if (startPointNumber == null) {
            startPointNumber = mostRecent.getStartPointNumber().orElse(null);
        }
        if (checkInTime == null) {
            checkInTime = mostRecent.getCheckInTime();
        }
        if (checkOutTime == null) {
            checkOutTime = mostRecent.getCheckOutTime();
        }
        if (neighborhoodId == null) {
            neighborhoodId = mostRecent.getNeighborhoodId().orElse(null);
        }
        populateDefaultAvailability(existing);
    }

    public List<AvailabilityPeriod> toAvailabilityPeriods() {
        final List<AvailabilityPeriod> result = new ArrayList<>();
        for (final AvailabilityRow row : availabilityRows) {
            result.add(new AvailabilityPeriod(row.getFrom(), row.getUntil()));
        }
        return result;
    }

    public List<BigDecimal> toPeriodPrices() {
        final List<BigDecimal> prices = new ArrayList<>();
        for (final AvailabilityRow row : availabilityRows) {
            prices.add(row.getDayPrice());
        }
        return prices;
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

        @ListingPricePerDay(groups = ValidationGroups.OnListingEdit.class)
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
