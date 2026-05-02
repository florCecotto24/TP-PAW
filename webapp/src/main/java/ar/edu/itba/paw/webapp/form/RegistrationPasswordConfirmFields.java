package ar.edu.itba.paw.webapp.form;

/** Marker for forms that submit {@code password} and {@code passwordConfirm} validated together. */
public interface RegistrationPasswordConfirmFields {

    String getPassword();

    String getPasswordConfirm();
}
