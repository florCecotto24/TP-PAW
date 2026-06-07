package ar.edu.itba.paw.webapp.form.user;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PastOrPresent;

import org.springframework.format.annotation.DateTimeFormat;

import ar.edu.itba.paw.webapp.validation.ValidationGroups;
import ar.edu.itba.paw.webapp.validation.constraint.user.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.constraint.user.OptionalCbu;
import ar.edu.itba.paw.webapp.validation.constraint.user.PhoneNumber;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength.Kind;
import ar.edu.itba.paw.webapp.validation.constraint.user.UserValidationMaxLength;

/** Profile POST: forename, surname, phone, optional CBU, and about text. */
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

    /**
     * Optional date of birth. Bound from the HTML5 {@code <input type="date">} (ISO {@code yyyy-MM-dd})
     * via {@link DateTimeFormat}. {@link PastOrPresent} forbids future dates; conversion failures are
     * caught by Spring's standard {@code typeMismatch.java.time.LocalDate} message.
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "{profile.birthDate.future}", groups = ValidationGroups.OnProfileUpdate.class)
    private LocalDate birthDate;

    @UserValidationMaxLength(
            kind = Kind.PROFILE_ABOUT,
            messageKey = "validation.profile.about.maxLength",
            groups = ValidationGroups.OnProfileUpdate.class)
    private String about = "";

    @OptionalCbu(groups = ValidationGroups.OnProfileUpdate.class)
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final LocalDate birthDate) {
        this.birthDate = birthDate;
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
