package ar.edu.itba.paw.webapp.validation.file;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.common.OctetStreamBodyForm;
import ar.edu.itba.paw.webapp.validation.constraint.file.NotEmptyPayload;

public final class NotEmptyPayloadValidator implements ConstraintValidator<NotEmptyPayload, OctetStreamBodyForm> {

    @Override
    public boolean isValid(final OctetStreamBodyForm form, final ConstraintValidatorContext context) {
        return form != null && form.getBytes() != null && form.getBytes().length > 0;
    }
}
