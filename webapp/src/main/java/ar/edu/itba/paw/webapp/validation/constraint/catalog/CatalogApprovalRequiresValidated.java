package ar.edu.itba.paw.webapp.validation.constraint.catalog;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import ar.edu.itba.paw.webapp.validation.catalog.CatalogApprovalRequiresValidatedValidator;

/** Admin catalog PATCH must set {@code validated} to {@code true}. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CatalogApprovalRequiresValidatedValidator.class)
public @interface CatalogApprovalRequiresValidated {

    String message() default "{validation.catalog.approval.required}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
