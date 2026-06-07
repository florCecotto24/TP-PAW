package ar.edu.itba.paw.webapp.form.user;

/** Marker for forms that submit {@code password} and {@code passwordConfirm} validated together. */
public interface RegistrationPasswordConfirmFields {

    String getPassword();

    String getPasswordConfirm();
}
