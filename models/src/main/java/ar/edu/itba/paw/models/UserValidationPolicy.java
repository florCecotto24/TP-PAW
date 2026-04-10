package ar.edu.itba.paw.models;

import java.util.regex.Pattern;


public final class UserValidationPolicy {

    private final int registrationPasswordMinLength;
    private final int profilePhoneMaxLength;
    private final Pattern profilePhonePattern;

    public UserValidationPolicy(
            final int registrationPasswordMinLength,
            final int profilePhoneMaxLength,
            final Pattern profilePhonePattern) {
        this.registrationPasswordMinLength = registrationPasswordMinLength;
        this.profilePhoneMaxLength = profilePhoneMaxLength;
        this.profilePhonePattern = profilePhonePattern;
    }

    public int getRegistrationPasswordMinLength() {
        return registrationPasswordMinLength;
    }

    public int getProfilePhoneMaxLength() {
        return profilePhoneMaxLength;
    }

    public Pattern getProfilePhonePattern() {
        return profilePhonePattern;
    }
}
