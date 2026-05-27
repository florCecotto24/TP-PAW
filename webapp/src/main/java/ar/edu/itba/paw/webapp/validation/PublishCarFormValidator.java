package ar.edu.itba.paw.webapp.validation;

import java.time.Year;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ar.edu.itba.paw.webapp.form.PublishCarForm;

/**
 * Cross-field validator for the publish-car flow:
 * - {@code type} is required only when the user creates a new catalog model (modelId == 0 / null), since
 *   an existing model already carries its body type.
 * - {@code year} (when provided) must be no greater than the current calendar year.
 */
@Component
public final class PublishCarFormValidator implements Validator {

    @Override
    public boolean supports(final Class<?> clazz) {
        return PublishCarForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        if (!(target instanceof PublishCarForm form)) {
            return;
        }
        final Long modelId = form.getModelId();
        final boolean creatingNewModel = modelId == null || modelId.longValue() == 0L;
        if (creatingNewModel && form.getType() == null) {
            errors.rejectValue("type", "validation.type.notNull");
        }
        final Integer year = form.getYear();
        if (year != null) {
            final int currentYear = Year.now().getValue();
            if (year > currentYear) {
                errors.rejectValue("year", "validation.year.max",
                        new Object[] { currentYear }, null);
            }
        }
    }
}
