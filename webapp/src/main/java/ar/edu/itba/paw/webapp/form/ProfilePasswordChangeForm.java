package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.RegistrationPasswordRules;

@RegistrationPasswordRules
public class ProfilePasswordChangeForm implements RegistrationPasswordConfirmFields {

    @NotBlank(message = "{profile.password.current.required}")
    private String currentPassword = "";

    @NotBlank(message = "{validation.registration.password.required}")
    private String password = "";

    @NotBlank(message = "{validation.registration.passwordConfirm.required}")
    private String passwordConfirm = "";

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(final String currentPassword) {
        this.currentPassword = currentPassword != null ? currentPassword : "";
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password != null ? password : "";
    }

    @Override
    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(final String passwordConfirm) {
        this.passwordConfirm = passwordConfirm != null ? passwordConfirm : "";
    }
}
