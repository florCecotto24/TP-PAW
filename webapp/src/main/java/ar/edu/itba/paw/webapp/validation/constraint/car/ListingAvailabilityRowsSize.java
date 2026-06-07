package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ListingAvailabilityRowsSizeValidator;

/**
 * {@code Size}-style bound on a {@code List<?>} of availability rows resolved at runtime
 * against {@link ar.edu.itba.paw.policy.ListingFormValidationPolicy}. Replaces hardcoded
 * {@code @Size(min = ..., max = 10)} usage so the count limit lives in
 * {@code app.validation.listing-availability-rows-*}.
 */
@Documented
@Constraint(validatedBy = ListingAvailabilityRowsSizeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ListingAvailabilityRowsSize {

    /**
     * If {@code true} the policy minimum is enforced (used by the create form, where at least
     * one row is required); when {@code false} only the maximum is enforced (edit form, which
     * tolerates an empty list because the controller maps that to "redirect to detail").
     */
    boolean enforceMinimum();

    /** Key in {@code messages*.properties} for the violation; interpolated with {@code {min}} and {@code {max}}. */
    String messageKey();

    String message() default "{javax.validation.constraints.Size.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
