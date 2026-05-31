package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.constraint.ValidCbu;

/**
 * Spring MVC binding for {@code POST /profile/cbu} (AJAX). The single {@link ValidCbu} field
 * replaces the controller's manual {@code if (!userService.isValidCbuFormat(...))} guard.
 */
public final class CbuUpdateForm {

    @ValidCbu
    private String cbu;

    public String getCbu() {
        return cbu;
    }

    public void setCbu(final String cbu) {
        this.cbu = cbu;
    }
}
