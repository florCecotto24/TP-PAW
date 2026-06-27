package ar.edu.itba.paw.webapp.form.catalog;

import ar.edu.itba.paw.webapp.validation.constraint.catalog.CatalogApprovalRequiresValidated;

/** Body for admin approval {@code PATCH /brands/{id}} and {@code PATCH /models/{id}}. */
@CatalogApprovalRequiresValidated
public final class CatalogApprovalForm {

    private Boolean validated;

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(final Boolean validated) {
        this.validated = validated;
    }
}
