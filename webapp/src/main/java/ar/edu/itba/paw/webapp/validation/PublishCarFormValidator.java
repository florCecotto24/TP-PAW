package ar.edu.itba.paw.webapp.validation;

import java.time.Year;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.webapp.form.PublishCarForm;

/**
 * Cross-field validator for the publish-car flow:
 * <ul>
 *   <li>{@code type} is required only when the user creates a new catalog model (modelId == 0 / null), since
 *       an existing model already carries its body type.</li>
 *   <li>{@code year} (when provided) must sit within {@code [policy.yearMin, currentYear]}; both bounds come
 *       from {@link CarValidationPolicy} so the form has no magic numbers.</li>
 * </ul>
 */
@Component
public final class PublishCarFormValidator implements Validator {

    private final CarValidationPolicy carValidationPolicy;

    public PublishCarFormValidator(final CarValidationPolicy carValidationPolicy) {
        this.carValidationPolicy = carValidationPolicy;
    }

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
            final int yearMin = carValidationPolicy.getYearMin();
            if (year < yearMin) {
                errors.rejectValue("year", "validation.year.min",
                        new Object[] { yearMin }, null);
                return;
            }
            final int currentYear = Year.now().getValue();
            if (year > currentYear) {
                errors.rejectValue("year", "validation.year.max",
                        new Object[] { currentYear }, null);
            }
        }
    }
}
