package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.validation.PhoneNumber;

public class ProfileUpdateForm {

    @PhoneNumber
    private String phoneNumber = "";
    private String birthDate = "";

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
