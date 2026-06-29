package ar.edu.itba.paw.webapp.validation.user;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.user.ValidIsoLocalDate;

public final class ValidIsoLocalDateValidator implements ConstraintValidator<ValidIsoLocalDate, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            LocalDate.parse(value.trim());
            return true;
        } catch (final DateTimeParseException ex) {
            return false;
        }
    }
}
