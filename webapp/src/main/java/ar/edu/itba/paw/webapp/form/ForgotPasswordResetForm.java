package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import ar.edu.itba.paw.webapp.validation.constraint.RegistrationPasswordRules;

@RegistrationPasswordRules
public class ForgotPasswordResetForm implements RegistrationPasswordConfirmFields {

    @NotBlank(message = "{forgotPassword.code.required}")
    @Pattern(regexp = "[0-9]{6}", message = "{forgotPassword.code.pattern}")
    private String code = "";

    @NotBlank(message = "{validation.registration.password.required}")
    private String password = "";

    @NotBlank(message = "{validation.registration.passwordConfirm.required}")
    private String passwordConfirm = "";

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code != null ? code.trim() : "";
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
