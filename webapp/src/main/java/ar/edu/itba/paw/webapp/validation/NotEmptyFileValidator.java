package ar.edu.itba.paw.webapp.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import ar.edu.itba.paw.webapp.validation.constraint.NotEmptyFile;

/** Bean Validation engine for {@link NotEmptyFile}. */
@Component
public final class NotEmptyFileValidator implements ConstraintValidator<NotEmptyFile, MultipartFile> {

    @Override
    public boolean isValid(final MultipartFile value, final ConstraintValidatorContext context) {
        return value != null && !value.isEmpty();
    }
}
