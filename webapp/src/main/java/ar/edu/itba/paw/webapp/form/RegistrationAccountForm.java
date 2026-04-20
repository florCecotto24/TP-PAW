package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import ar.edu.itba.paw.webapp.validation.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.RegistrationPasswordRules;

@RegistrationPasswordRules
public class RegistrationAccountForm implements RegistrationPasswordConfirmFields {

    @NotBlank(message = "{register.forename.required}")
    @Size(max = 50)
    @NoPunctuation
    private String forename = "";

    @NotBlank(message = "{register.surname.required}")
    @Size(max = 50)
    @NoPunctuation
    private String surname = "";

    @NotBlank(message = "{register.email.required}")
    @Email(message = "{register.email.invalid}")
    @Size(max = 50)
    private String email = "";

    @NotBlank(message = "{validation.registration.password.required}")
    private String password = "";

    @NotBlank(message = "{validation.registration.passwordConfirm.required}")
    private String passwordConfirm = "";

    public String getForename() {
        return forename;
    }

    public void setForename(final String forename) {
        this.forename = forename != null ? forename : "";
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname != null ? surname : "";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email != null ? email : "";
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
