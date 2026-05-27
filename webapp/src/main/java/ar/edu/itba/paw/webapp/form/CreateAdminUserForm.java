package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength.Kind;

public final class CreateAdminUserForm {

    @NotBlank(message = "{register.forename.required}", groups = ValidationGroups.OnCreateAdminUser.class)
    @UserValidationMaxLength(
            kind = Kind.DISPLAY_NAME_PART,
            messageKey = "validation.registration.forename.maxLength",
            groups = ValidationGroups.OnCreateAdminUser.class)
    @NoPunctuation(groups = ValidationGroups.OnCreateAdminUser.class)
    private String forename = "";

    @NotBlank(message = "{register.surname.required}", groups = ValidationGroups.OnCreateAdminUser.class)
    @UserValidationMaxLength(
            kind = Kind.DISPLAY_NAME_PART,
            messageKey = "validation.registration.surname.maxLength",
            groups = ValidationGroups.OnCreateAdminUser.class)
    @NoPunctuation(groups = ValidationGroups.OnCreateAdminUser.class)
    private String surname = "";

    @NotBlank(message = "{register.email.required}", groups = ValidationGroups.OnCreateAdminUser.class)
    @Email(message = "{register.email.invalid}", groups = ValidationGroups.OnCreateAdminUser.class)
    @UserValidationMaxLength(
            kind = Kind.REGISTRATION_EMAIL,
            messageKey = "validation.registration.email.maxLength",
            groups = ValidationGroups.OnCreateAdminUser.class)
    private String email = "";

    @NotBlank(message = "{validation.registration.password.required}", groups = ValidationGroups.OnCreateAdminUser.class)
    private String password = "";

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
        this.email = email != null ? email.trim() : "";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password != null ? password : "";
    }
}
