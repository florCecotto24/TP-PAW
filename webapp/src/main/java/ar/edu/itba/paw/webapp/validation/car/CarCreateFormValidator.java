package ar.edu.itba.paw.webapp.validation.car;

import java.time.Year;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ar.edu.itba.paw.policy.CarValidationPolicy;
import ar.edu.itba.paw.webapp.form.car.CarCreateForm;
import ar.edu.itba.paw.webapp.support.CarRestEnums;
import ar.edu.itba.paw.webapp.util.ModelUriSupport;

/**
 * Cross-field validator for REST {@link CarCreateForm}: catalog fields when {@code modelUri} is absent,
 * optional {@code year} bounds from {@link CarValidationPolicy}.
 */
@Component
public final class CarCreateFormValidator implements Validator {

    private final CarValidationPolicy carValidationPolicy;

    public CarCreateFormValidator(final CarValidationPolicy carValidationPolicy) {
        this.carValidationPolicy = carValidationPolicy;
    }

    @Override
    public boolean supports(final Class<?> clazz) {
        return CarCreateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(final Object target, final Errors errors) {
        if (!(target instanceof CarCreateForm form)) {
            return;
        }
        validateCatalogFields(form, errors);
        validateYear(form, errors);
    }

    private void validateCatalogFields(final CarCreateForm form, final Errors errors) {
        if (form.getModelUri() != null && !form.getModelUri().isBlank()) {
            if (!isValidModelUri(form.getModelUri())) {
                errors.rejectValue("modelUri", "validation.modelUri.invalid");
            }
            return;
        }
        if (form.getBrandName() == null || form.getBrandName().isBlank()) {
            errors.rejectValue("brandName", "validation.brand.notBlank");
        }
        if (form.getModelName() == null || form.getModelName().isBlank()) {
            errors.rejectValue("modelName", "validation.model.notBlank");
        }
        if (form.getType() == null || form.getType().isBlank()) {
            errors.rejectValue("type", "validation.type.notNull");
        } else {
            try {
                if (CarRestEnums.parseType(form.getType()) == null) {
                    errors.rejectValue("type", "validation.type.unknown");
                }
            } catch (final IllegalArgumentException ex) {
                errors.rejectValue("type", "validation.type.unknown");
            }
        }
    }

    private void validateYear(final CarCreateForm form, final Errors errors) {
        final Integer year = form.getYear();
        if (year == null) {
            return;
        }
        final int yearMin = carValidationPolicy.getYearMin();
        if (year < yearMin) {
            errors.rejectValue("year", "validation.year.min", new Object[] { yearMin }, null);
            return;
        }
        final int currentYear = Year.now().getValue();
        if (year > currentYear) {
            errors.rejectValue("year", "validation.year.max", new Object[] { currentYear }, null);
        }
    }

    private static boolean isValidModelUri(final String modelUri) {
        try {
            ModelUriSupport.parseModelId(modelUri);
            return true;
        } catch (final RuntimeException ex) {
            return false;
        }
    }
}
