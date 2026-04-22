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

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.webapp.validation.CheckOutAfterCheckIn;

@CheckOutAfterCheckIn
public class ListingEditForm implements ListingTimeWindow {

    @NotNull(message = "{validation.pricePerDay.notNull}")
    @DecimalMin(value = "0.01", message = "{validation.pricePerDay.decimalMin}")
    @Digits(integer = 8, fraction = 2, message = "{validation.pricePerDay.digits}")
    private BigDecimal pricePerDay;

    @NotBlank(message = "{validation.startPoint.notBlank}")
    @Size(max = 250, message = "{validation.startPoint.size}")
    private String startPoint;

    @Size(max = 200, message = "{validation.description.size}")
    private String description;

    @NotNull(message = "{validation.checkInTime.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    @NotNull(message = "{validation.checkOutTime.notNull}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkOutTime;

    @Size(min = 1, max = 10, message = "{validation.availabilityRows.size.range}")
    private List<@Valid AvailabilityRow> availabilityRows = new ArrayList<>();

    public ListingEditForm() {
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
        @NotNull(message = "{validation.availabilityRow.from.notNull}")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate from;

        @NotNull(message = "{validation.availabilityRow.until.notNull}")
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

    public List<AvailabilityRow> getAvailabilityRows() {
        return availabilityRows;
    }

    public void setAvailabilityRows(final List<AvailabilityRow> availabilityRows) {
        this.availabilityRows = availabilityRows;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(final BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(final String startPoint) {
        this.startPoint = startPoint;
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
