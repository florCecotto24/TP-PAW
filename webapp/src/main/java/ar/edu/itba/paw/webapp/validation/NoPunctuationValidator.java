package ar.edu.itba.paw.webapp.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.validation.constraint.NoPunctuation;

public final class NoPunctuationValidator implements ConstraintValidator<NoPunctuation, String> {
    @Override
    public void initialize(NoPunctuation constraintAnnotation) {
    }
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.matches("^[\\p{L}\\p{M}\\p{N}\\s]*$");
    }
}
