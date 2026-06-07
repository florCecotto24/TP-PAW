package ar.edu.itba.paw.policy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Central limits for listing creation/edit forms (handover address, availability rows count,
 * minimum rental days, price-per-day numeric range and digit precision).
 * Aligned with the {@code app.validation.listing-*} keys.
 */
public final class ListingFormValidationPolicy {

    private final int addressStreetMaxLength;
    private final int addressNumberMaxLength;
    private final int availabilityRowsMin;
    private final int availabilityRowsMax;
    private final int minimumRentalDaysMin;
    private final int minimumRentalDaysMax;
    private final BigDecimal pricePerDayMin;
    private final int pricePerDayIntegerDigits;
    private final int pricePerDayFractionDigits;

    public static ListingFormValidationPolicy fromValidatedConfiguration(
            final int addressStreetMaxLength,
            final int addressNumberMaxLength,
            final int availabilityRowsMin,
            final int availabilityRowsMax,
            final int minimumRentalDaysMin,
            final int minimumRentalDaysMax,
            final BigDecimal pricePerDayMin,
            final int pricePerDayIntegerDigits,
            final int pricePerDayFractionDigits) {
        requireAtLeast("app.validation.listing-address-street-max-length", addressStreetMaxLength, 1);
        requireAtLeast("app.validation.listing-address-number-max-length", addressNumberMaxLength, 1);
        requireAtLeast("app.validation.listing-availability-rows-min", availabilityRowsMin, 0);
        requireMaxNotBelowMin(
                "app.validation.listing-availability-rows-max", availabilityRowsMax, Math.max(availabilityRowsMin, 1));
        requireAtLeast("app.validation.listing-minimum-rental-days-min", minimumRentalDaysMin, 1);
        requireMaxNotBelowMin(
                "app.validation.listing-minimum-rental-days-max", minimumRentalDaysMax, minimumRentalDaysMin);
        Objects.requireNonNull(pricePerDayMin, "app.validation.listing-price-per-day-min");
        if (pricePerDayMin.signum() <= 0) {
            throw new IllegalArgumentException(
                    "app.validation.listing-price-per-day-min must be > 0, got " + pricePerDayMin.toPlainString());
        }
        requireAtLeast("app.validation.listing-price-per-day-integer-digits", pricePerDayIntegerDigits, 1);
        requireAtLeast("app.validation.listing-price-per-day-fraction-digits", pricePerDayFractionDigits, 0);
        return new ListingFormValidationPolicy(
                addressStreetMaxLength,
                addressNumberMaxLength,
                availabilityRowsMin,
                availabilityRowsMax,
                minimumRentalDaysMin,
                minimumRentalDaysMax,
                pricePerDayMin,
                pricePerDayIntegerDigits,
                pricePerDayFractionDigits);
    }

    private static void requireAtLeast(final String propertyKey, final int value, final int minInclusive) {
        if (value < minInclusive) {
            throw new IllegalArgumentException(propertyKey + " must be >= " + minInclusive + ", got " + value);
        }
    }

    private static void requireMaxNotBelowMin(final String propertyKey, final int max, final int min) {
        if (max < min) {
            throw new IllegalArgumentException(propertyKey + " must be >= min, got " + max + " < " + min);
        }
    }

    private ListingFormValidationPolicy(
            final int addressStreetMaxLength,
            final int addressNumberMaxLength,
            final int availabilityRowsMin,
            final int availabilityRowsMax,
            final int minimumRentalDaysMin,
            final int minimumRentalDaysMax,
            final BigDecimal pricePerDayMin,
            final int pricePerDayIntegerDigits,
            final int pricePerDayFractionDigits) {
        this.addressStreetMaxLength = addressStreetMaxLength;
        this.addressNumberMaxLength = addressNumberMaxLength;
        this.availabilityRowsMin = availabilityRowsMin;
        this.availabilityRowsMax = availabilityRowsMax;
        this.minimumRentalDaysMin = minimumRentalDaysMin;
        this.minimumRentalDaysMax = minimumRentalDaysMax;
        this.pricePerDayMin = pricePerDayMin;
        this.pricePerDayIntegerDigits = pricePerDayIntegerDigits;
        this.pricePerDayFractionDigits = pricePerDayFractionDigits;
    }

    public int getAddressStreetMaxLength() {
        return addressStreetMaxLength;
    }

    public int getAddressNumberMaxLength() {
        return addressNumberMaxLength;
    }

    public int getAvailabilityRowsMin() {
        return availabilityRowsMin;
    }

    public int getAvailabilityRowsMax() {
        return availabilityRowsMax;
    }

    public int getMinimumRentalDaysMin() {
        return minimumRentalDaysMin;
    }

    public int getMinimumRentalDaysMax() {
        return minimumRentalDaysMax;
    }

    public BigDecimal getPricePerDayMin() {
        return pricePerDayMin;
    }

    public int getPricePerDayIntegerDigits() {
        return pricePerDayIntegerDigits;
    }

    public int getPricePerDayFractionDigits() {
        return pricePerDayFractionDigits;
    }

    /**
     * Maximum value derived from the configured digit precision (e.g. {@code 99999999.99} when
     * integer-digits=8 and fraction-digits=2). Useful for HTML {@code <input max="...">}
     * attributes whose value must mirror the {@code @Digits} bound on the form.
     */
    public BigDecimal getPricePerDayMaxValue() {
        final BigDecimal integerPart = BigDecimal.TEN.pow(pricePerDayIntegerDigits).subtract(BigDecimal.ONE);
        if (pricePerDayFractionDigits == 0) {
            return integerPart;
        }
        final BigDecimal fractionPart = BigDecimal.ONE.subtract(
                BigDecimal.TEN.pow(-pricePerDayFractionDigits, MathContext.DECIMAL64));
        return integerPart.add(fractionPart).setScale(pricePerDayFractionDigits, RoundingMode.DOWN);
    }
}
