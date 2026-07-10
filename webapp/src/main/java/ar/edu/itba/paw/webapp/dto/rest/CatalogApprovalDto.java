package ar.edu.itba.paw.webapp.dto.rest;

import ar.edu.itba.paw.webapp.validation.constraint.catalog.CatalogApprovalRequiresValidated;

/** Body for admin approval {@code PATCH /brands/{id}} and {@code PATCH /brands/{id}/models/{modelId}}. */
@CatalogApprovalRequiresValidated
public final class CatalogApprovalDto {

    private Boolean validated;

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(final Boolean validated) {
        this.validated = validated;
    }
}
