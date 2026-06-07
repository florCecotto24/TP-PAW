package ar.edu.itba.paw.webapp.validation.constraint.car;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.car.ListingFormValidationSizeValidator;

/**
 * Length bound resolved at runtime against {@link ar.edu.itba.paw.policy.ListingFormValidationPolicy}
 * (handover street / number for listing creation and edit forms).
 */
@Documented
@Constraint(validatedBy = ListingFormValidationSizeValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ListingFormValidationSize {

    Kind kind();

    /** Key in {@code messages*.properties}; interpolated with {@code {0}=max}. */
    String messageKey();

    String message() default "{javax.validation.constraints.Size.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum Kind {
        ADDRESS_STREET,
        ADDRESS_NUMBER
    }
}
