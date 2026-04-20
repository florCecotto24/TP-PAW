package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import ar.edu.itba.paw.webapp.validation.NoPunctuation;
import ar.edu.itba.paw.webapp.validation.PhoneNumber;

public class ProfileUpdateForm {

    @NotBlank(message = "{profile.forename.required}")
    @Size(max = 50, message = "{profile.forename.size}")
    @NoPunctuation
    private String forename = "";

    @NotBlank(message = "{profile.surname.required}")
    @Size(max = 50, message = "{profile.surname.size}")
    @NoPunctuation
    private String surname = "";

    @PhoneNumber
    private String phoneNumber = "";
    private String birthDate = "";

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
}
