package ar.edu.itba.paw.webapp.support;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.springframework.stereotype.Component;

/**
 * Runs Bean Validation with explicit groups on REST request bodies (same groups as legacy MVC forms).
 */
@Component
public final class FormValidationSupport {

    private final Validator validator;

    public FormValidationSupport(final Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(final T form, final Class<?>... groups) {
        final Set<ConstraintViolation<T>> violations = validator.validate(form, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
