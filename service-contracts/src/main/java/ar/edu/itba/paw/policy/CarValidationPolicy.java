package ar.edu.itba.paw.policy;

/**
 * Central limits for car publishing text inputs (brand, model, plate, description) and year floor.
 * Aligned with the {@code app.validation.car-*} keys. Built via
 * {@link #fromValidatedConfiguration(int, int, int, int, int, int, int)} so invariants are checked once at boot.
 */
public final class CarValidationPolicy {

    private final int brandMinLength;
    private final int brandMaxLength;
    private final int modelMaxLength;
    private final int plateMinLength;
    private final int plateMaxLength;
    private final int descriptionMaxLength;
    private final int yearMin;

    public static CarValidationPolicy fromValidatedConfiguration(
            final int brandMinLength,
            final int brandMaxLength,
            final int modelMaxLength,
            final int plateMinLength,
            final int plateMaxLength,
            final int descriptionMaxLength,
            final int yearMin) {
        requireAtLeast("app.validation.car-brand-min-length", brandMinLength, 1);
        requireMaxNotBelowMin("app.validation.car-brand-max-length", brandMaxLength, brandMinLength);
        requireAtLeast("app.validation.car-model-max-length", modelMaxLength, 1);
        requireAtLeast("app.validation.car-plate-min-length", plateMinLength, 1);
        requireMaxNotBelowMin("app.validation.car-plate-max-length", plateMaxLength, plateMinLength);
        requireAtLeast("app.validation.car-description-max-length", descriptionMaxLength, 1);
        requireAtLeast("app.validation.car-year-min", yearMin, 1);
        return new CarValidationPolicy(
                brandMinLength,
                brandMaxLength,
                modelMaxLength,
                plateMinLength,
                plateMaxLength,
                descriptionMaxLength,
                yearMin);
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

    private CarValidationPolicy(
            final int brandMinLength,
            final int brandMaxLength,
            final int modelMaxLength,
            final int plateMinLength,
            final int plateMaxLength,
            final int descriptionMaxLength,
            final int yearMin) {
        this.brandMinLength = brandMinLength;
        this.brandMaxLength = brandMaxLength;
        this.modelMaxLength = modelMaxLength;
        this.plateMinLength = plateMinLength;
        this.plateMaxLength = plateMaxLength;
        this.descriptionMaxLength = descriptionMaxLength;
        this.yearMin = yearMin;
    }

    public int getBrandMinLength() {
        return brandMinLength;
    }

    public int getBrandMaxLength() {
        return brandMaxLength;
    }

    public int getModelMaxLength() {
        return modelMaxLength;
    }

    public int getPlateMinLength() {
        return plateMinLength;
    }

    public int getPlateMaxLength() {
        return plateMaxLength;
    }

    public int getDescriptionMaxLength() {
        return descriptionMaxLength;
    }

    public int getYearMin() {
        return yearMin;
    }
}
