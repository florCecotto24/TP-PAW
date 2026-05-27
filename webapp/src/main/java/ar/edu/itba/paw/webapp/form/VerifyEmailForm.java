package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength.Kind;

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
    @Pattern(regexp = "[0-9]{6}", message = "{forgotPassword.code.pattern}", groups = ValidationGroups.OnVerifyEmail.class)
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
