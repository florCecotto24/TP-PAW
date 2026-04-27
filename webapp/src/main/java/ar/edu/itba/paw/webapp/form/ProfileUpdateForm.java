package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.constraint.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.constraint.PhoneNumber;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength.Kind;

public class ProfileUpdateForm {

    @NotBlank(message = "{profile.forename.required}")
    @UserValidationMaxLength(kind = Kind.DISPLAY_NAME_PART, messageKey = "validation.profile.forename.maxLength")
    @NoPunctuation
    private String forename = "";

    @NotBlank(message = "{profile.surname.required}")
    @UserValidationMaxLength(kind = Kind.DISPLAY_NAME_PART, messageKey = "validation.profile.surname.maxLength")
    @NoPunctuation
    private String surname = "";

    @PhoneNumber
    private String phoneNumber = "";
    private String birthDate = "";
    @UserValidationMaxLength(kind = Kind.PROFILE_ABOUT, messageKey = "validation.profile.about.maxLength")
    private String about = "";

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final String birthDate) {
        this.birthDate = birthDate != null ? birthDate : "";
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(final String about) {
        this.about = about != null ? about : "";
    }
}
