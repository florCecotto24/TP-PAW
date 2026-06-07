package ar.edu.itba.paw.webapp.validation.constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.ListingPricePerDayValidator;

/**
 * Numeric range and digit-precision bound resolved at runtime against
 * {@link ar.edu.itba.paw.policy.ListingFormValidationPolicy} (price-per-day for listing
 * creation and edit forms). Replaces the hardcoded
 * {@code @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)} pair so all listing-price
 * limits live in {@code app.validation.listing-price-per-day-*}.
 *
 * <p>{@code null} is considered valid so callers can still opt in to a separate
 * {@code @NotNull} when the price is required (matching the previous behaviour of
 * {@code @DecimalMin}/{@code @Digits}).</p>
 */
@Documented
@Constraint(validatedBy = ListingPricePerDayValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ListingPricePerDay {

    /** Key in {@code messages*.properties} for the "below minimum" violation; interpolated with {@code {min}}. */
    String belowMinMessageKey() default "validation.pricePerDay.decimalMin";

    /**
     * Key in {@code messages*.properties} for the "out of digit precision" violation;
     * interpolated with {@code {integer}} and {@code {fraction}}.
     */
    String digitsMessageKey() default "validation.pricePerDay.digits";

    String message() default "{javax.validation.constraints.DecimalMin.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
