package ar.edu.itba.paw.webapp.form.user;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength.Kind;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.user.VerificationCode;

public final class VerifyEmailForm {

    @NotBlank(message = "{register.email.required}",
            groups = {ValidationGroups.OnVerifyEmail.class, ValidationGroups.OnResendVerification.class})
    @Email(message = "{register.email.invalid}",
            groups = {ValidationGroups.OnVerifyEmail.class, ValidationGroups.OnResendVerification.class})
    @UserValidationMaxLength(
            kind = Kind.REGISTRATION_EMAIL,
            messageKey = "validation.registration.email.maxLength",
            groups = {ValidationGroups.OnVerifyEmail.class, ValidationGroups.OnResendVerification.class})
    private String email = "";

    @NotBlank(message = "{forgotPassword.code.required}", groups = ValidationGroups.OnVerifyEmail.class)
    @VerificationCode(messageKey = "forgotPassword.code.pattern", groups = ValidationGroups.OnVerifyEmail.class)
    private String code = "";

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email != null ? email.trim() : "";
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code != null ? code.trim() : "";
    }
}
