package ar.edu.itba.paw.webapp.validation.user;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.user.ValidUserRole;

public final class ValidUserRoleValidator implements ConstraintValidator<ValidUserRole, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        final String normalized = value.trim();
        return "admin".equalsIgnoreCase(normalized) || "user".equalsIgnoreCase(normalized);
    }
}
