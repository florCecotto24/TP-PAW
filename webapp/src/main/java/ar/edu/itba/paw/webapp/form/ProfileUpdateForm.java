package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.constraint.PhoneNumber;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.UserValidationMaxLength.Kind;

public final class ProfileUpdateForm {

    @NotBlank(message = "{profile.forename.required}", groups = ValidationGroups.OnProfileUpdate.class)
    @UserValidationMaxLength(
            kind = Kind.DISPLAY_NAME_PART,
            messageKey = "validation.profile.forename.maxLength",
            groups = ValidationGroups.OnProfileUpdate.class)
    @NoPunctuation(groups = ValidationGroups.OnProfileUpdate.class)
    private String forename = "";

    @NotBlank(message = "{profile.surname.required}", groups = ValidationGroups.OnProfileUpdate.class)
    @UserValidationMaxLength(
            kind = Kind.DISPLAY_NAME_PART,
            messageKey = "validation.profile.surname.maxLength",
            groups = ValidationGroups.OnProfileUpdate.class)
    @NoPunctuation(groups = ValidationGroups.OnProfileUpdate.class)
    private String surname = "";

    @PhoneNumber(groups = ValidationGroups.OnProfileUpdate.class)
    private String phoneNumber = "";
    private String birthDate = "";

    @UserValidationMaxLength(
            kind = Kind.PROFILE_ABOUT,
            messageKey = "validation.profile.about.maxLength",
            groups = ValidationGroups.OnProfileUpdate.class)
    private String about = "";

    @Pattern(regexp = "\\d{22}", message = "{profile.cbu.size}")
    private String cbu;

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

    public String getCbu() {
        return cbu;
    }

    public void setCbu(final String cbu) {
        this.cbu = cbu != null ? cbu : "";
    }
}
