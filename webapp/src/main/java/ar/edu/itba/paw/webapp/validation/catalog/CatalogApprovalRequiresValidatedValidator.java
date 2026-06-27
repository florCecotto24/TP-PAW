package ar.edu.itba.paw.webapp.validation.catalog;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.form.catalog.CatalogApprovalForm;
import ar.edu.itba.paw.webapp.validation.constraint.catalog.CatalogApprovalRequiresValidated;

public final class CatalogApprovalRequiresValidatedValidator
        implements ConstraintValidator<CatalogApprovalRequiresValidated, CatalogApprovalForm> {

    @Override
    public boolean isValid(final CatalogApprovalForm form, final ConstraintValidatorContext context) {
        return form != null && Boolean.TRUE.equals(form.getValidated());
    }
}
