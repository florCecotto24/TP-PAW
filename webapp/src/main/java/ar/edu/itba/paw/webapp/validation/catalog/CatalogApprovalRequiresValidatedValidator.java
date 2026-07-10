package ar.edu.itba.paw.webapp.validation.catalog;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import ar.edu.itba.paw.webapp.dto.rest.CatalogApprovalDto;
import ar.edu.itba.paw.webapp.validation.constraint.catalog.CatalogApprovalRequiresValidated;

public final class CatalogApprovalRequiresValidatedValidator
        implements ConstraintValidator<CatalogApprovalRequiresValidated, CatalogApprovalDto> {

    @Override
    public boolean isValid(final CatalogApprovalDto dto, final ConstraintValidatorContext context) {
        return dto != null && Boolean.TRUE.equals(dto.getValidated());
    }
}
