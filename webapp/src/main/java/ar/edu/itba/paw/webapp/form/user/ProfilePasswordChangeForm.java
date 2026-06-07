package ar.edu.itba.paw.webapp.form.user;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.user.RegistrationPasswordRules;

/** Profile password change: current password plus new password and confirmation. */
@RegistrationPasswordRules(groups = ValidationGroups.OnProfilePassword.class)
public final class ProfilePasswordChangeForm implements RegistrationPasswordConfirmFields {

    @NotBlank(message = "{profile.password.current.required}", groups = ValidationGroups.OnProfilePassword.class)
    private String currentPassword = "";

    @NotBlank(message = "{validation.registration.password.required}", groups = ValidationGroups.OnProfilePassword.class)
    private String password = "";

    @NotBlank(message = "{validation.registration.passwordConfirm.required}", groups = ValidationGroups.OnProfilePassword.class)
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
