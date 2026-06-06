package ar.edu.itba.paw.policy;

/**
 * Central limits for listing creation/edit forms (handover address, availability rows count, minimum rental days).
 * Aligned with the {@code app.validation.listing-*} keys.
 */
public final class ListingFormValidationPolicy {

    private final int addressStreetMaxLength;
    private final int addressNumberMaxLength;
    private final int availabilityRowsMin;
    private final int availabilityRowsMax;
    private final int minimumRentalDaysMax;

    public static ListingFormValidationPolicy fromValidatedConfiguration(
            final int addressStreetMaxLength,
            final int addressNumberMaxLength,
            final int availabilityRowsMin,
            final int availabilityRowsMax,
            final int minimumRentalDaysMax) {
        requireAtLeast("app.validation.listing-address-street-max-length", addressStreetMaxLength, 1);
        requireAtLeast("app.validation.listing-address-number-max-length", addressNumberMaxLength, 1);
        requireAtLeast("app.validation.listing-availability-rows-min", availabilityRowsMin, 0);
        requireMaxNotBelowMin(
                "app.validation.listing-availability-rows-max", availabilityRowsMax, Math.max(availabilityRowsMin, 1));
        requireAtLeast("app.validation.listing-minimum-rental-days-max", minimumRentalDaysMax, 1);
        return new ListingFormValidationPolicy(
                addressStreetMaxLength,
                addressNumberMaxLength,
                availabilityRowsMin,
                availabilityRowsMax,
                minimumRentalDaysMax);
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
            final int minimumRentalDaysMax) {
        this.addressStreetMaxLength = addressStreetMaxLength;
        this.addressNumberMaxLength = addressNumberMaxLength;
        this.availabilityRowsMin = availabilityRowsMin;
        this.availabilityRowsMax = availabilityRowsMax;
        this.minimumRentalDaysMax = minimumRentalDaysMax;
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

    public int getMinimumRentalDaysMax() {
        return minimumRentalDaysMax;
    }
}
