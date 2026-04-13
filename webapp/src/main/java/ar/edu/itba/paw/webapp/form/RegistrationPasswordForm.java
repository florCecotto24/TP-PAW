package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.RegistrationPasswordRules;

@RegistrationPasswordRules
public class RegistrationPasswordForm implements RegistrationPasswordConfirmFields {

    @NotBlank(message = "{validation.registration.password.required}")
    private String password = "";

    @NotBlank(message = "{validation.registration.passwordConfirm.required}")
    private String passwordConfirm = "";

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password != null ? password : "";
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(final String passwordConfirm) {
        this.passwordConfirm = passwordConfirm != null ? passwordConfirm : "";
    }
}
