package ar.edu.itba.paw.services.policy;

/**
 * Central limits for the rider {@code ReservationForm}: hidden datetime strings, free-text delivery location and
 * display-only car name. Aligned with the {@code app.validation.reservation-*} keys.
 */
public final class ReservationFormValidationPolicy {

    private final int deliveryLocationMaxLength;
    private final int carNameMaxLength;
    private final int datetimeInputMaxLength;

    public static ReservationFormValidationPolicy fromValidatedConfiguration(
            final int deliveryLocationMaxLength,
            final int carNameMaxLength,
            final int datetimeInputMaxLength) {
        requireAtLeast(
                "app.validation.reservation-delivery-location-max-length", deliveryLocationMaxLength, 1);
        requireAtLeast("app.validation.reservation-car-name-max-length", carNameMaxLength, 1);
        requireAtLeast(
                "app.validation.reservation-datetime-input-max-length", datetimeInputMaxLength, 1);
        return new ReservationFormValidationPolicy(
                deliveryLocationMaxLength, carNameMaxLength, datetimeInputMaxLength);
    }

    private static void requireAtLeast(final String propertyKey, final int value, final int minInclusive) {
        if (value < minInclusive) {
            throw new IllegalArgumentException(propertyKey + " must be >= " + minInclusive + ", got " + value);
        }
    }

    private ReservationFormValidationPolicy(
            final int deliveryLocationMaxLength,
            final int carNameMaxLength,
            final int datetimeInputMaxLength) {
        this.deliveryLocationMaxLength = deliveryLocationMaxLength;
        this.carNameMaxLength = carNameMaxLength;
        this.datetimeInputMaxLength = datetimeInputMaxLength;
    }

    public int getDeliveryLocationMaxLength() {
        return deliveryLocationMaxLength;
    }

    public int getCarNameMaxLength() {
        return carNameMaxLength;
    }

    public int getDatetimeInputMaxLength() {
        return datetimeInputMaxLength;
    }
}
