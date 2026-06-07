package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ListingMinimumRentalDaysValidator;

/**
 * Numeric range bound on the {@code minimumRentalDays} listing form field, resolved at runtime
 * against {@link ar.edu.itba.paw.policy.ListingFormValidationPolicy}. Replaces the hardcoded
 * {@code @Min(1) @Max(365)} pair so both ends live in
 * {@code app.validation.listing-minimum-rental-days-{min,max}}.
 */
@Documented
@Constraint(validatedBy = ListingMinimumRentalDaysValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ListingMinimumRentalDays {

    /** Key in {@code messages*.properties} for the violation; interpolated with {@code {min}} and {@code {max}}. */
    String messageKey() default "validation.minimumRentalDays.range";

    String message() default "{javax.validation.constraints.Min.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
