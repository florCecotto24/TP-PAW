package ar.edu.itba.paw.webapp.validation.common;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.common.ValidYearMonth;

public final class ValidYearMonthValidator implements ConstraintValidator<ValidYearMonth, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            YearMonth.parse(value.trim());
            return true;
        } catch (final DateTimeParseException ex) {
            return false;
        }
    }
}
