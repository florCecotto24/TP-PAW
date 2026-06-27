package ar.edu.itba.paw.webapp.form.user;

import ar.edu.itba.paw.webapp.validation.constraint.user.OptionalCbu;
import ar.edu.itba.paw.webapp.validation.constraint.user.PhoneNumber;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserPatchHasField;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength.Kind;

/**
 * Partial update body for {@code PATCH /users/{id}}.
 * OTP credentials travel in {@code Authorization: Basic email:code}, never in the JSON body.
 */
@UserPatchHasField
public final class UserPatchForm {

    @UserValidationMaxLength(kind = Kind.DISPLAY_NAME_PART, messageKey = "validation.profile.forename.maxLength")
    private String forename;

    @UserValidationMaxLength(kind = Kind.DISPLAY_NAME_PART, messageKey = "validation.profile.surname.maxLength")
    private String surname;

    @PhoneNumber
    private String phoneNumber;
    private String birthDate;

    @UserValidationMaxLength(kind = Kind.PROFILE_ABOUT, messageKey = "validation.profile.about.maxLength")
    private String about;

    @OptionalCbu
    private String cbu;
    private String latestLocale;
    private String password;
    private String passwordConfirm;
    private String currentPassword;
    private String role;
    private Boolean blocked;
    private Boolean identityValidated;
    private Boolean licenseValidated;

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

    public String getLatestLocale() {
        return latestLocale;
    }

    public void setLatestLocale(final String latestLocale) {
        this.latestLocale = latestLocale;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(final String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(final String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(final Boolean blocked) {
        this.blocked = blocked;
    }

    public Boolean getIdentityValidated() {
        return identityValidated;
    }

    public void setIdentityValidated(final Boolean identityValidated) {
        this.identityValidated = identityValidated;
    }

    public Boolean getLicenseValidated() {
        return licenseValidated;
    }

    public void setLicenseValidated(final Boolean licenseValidated) {
        this.licenseValidated = licenseValidated;
    }
}
