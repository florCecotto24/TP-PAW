package ar.edu.itba.paw.webapp.form.user;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength.Kind;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength;

public final class ForgotPasswordRequestForm {

    @NotBlank(message = "{register.email.required}", groups = ValidationGroups.OnForgotPasswordRequest.class)
    @Email(message = "{register.email.invalid}", groups = ValidationGroups.OnForgotPasswordRequest.class)
    @UserValidationMaxLength(
            kind = Kind.REGISTRATION_EMAIL,
            messageKey = "validation.registration.email.maxLength",
            groups = ValidationGroups.OnForgotPasswordRequest.class)
    private String email = "";

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email != null ? email.trim() : "";
    }
}
