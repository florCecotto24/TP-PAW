package ar.edu.itba.paw.webapp.form.user;

import javax.validation.constraints.NotBlank;

import ar.edu.itba.paw.webapp.validation.constraint.user.OptionalCbu;
import ar.edu.itba.paw.webapp.validation.constraint.user.PhoneNumber;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.user.ValidIsoLocalDate;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength.Kind;

/** REST body for {@code PUT /users/{id}} (self/admin replaces the editable profile fields). */
public final class UserReplaceForm {

    @NotBlank(message = "{register.forename.required}")
    @UserValidationMaxLength(kind = Kind.DISPLAY_NAME_PART, messageKey = "validation.profile.forename.maxLength")
    private String forename;

    @NotBlank(message = "{register.surname.required}")
    @UserValidationMaxLength(kind = Kind.DISPLAY_NAME_PART, messageKey = "validation.profile.surname.maxLength")
    private String surname;

    @PhoneNumber
    private String phoneNumber;

    @ValidIsoLocalDate
    private String birthDate;

    @UserValidationMaxLength(kind = Kind.PROFILE_ABOUT, messageKey = "validation.profile.about.maxLength")
    private String about;

    @OptionalCbu
    private String cbu;

    public String getForename() {
        return forename;
    }

    public void setForename(final String forename) {
        this.forename = forename;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(final String about) {
        this.about = about;
    }

    public String getCbu() {
        return cbu;
    }

    public void setCbu(final String cbu) {
        this.cbu = cbu;
    }
}
