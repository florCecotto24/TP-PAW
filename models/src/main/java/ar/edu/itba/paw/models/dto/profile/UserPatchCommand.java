package ar.edu.itba.paw.models.dto.profile;

import java.time.LocalDate;

/**
 * Partial user update fields for {@code PATCH /users/{id}} (profile and admin facets).
 * Only non-null properties are applied; password changes use a separate service entry point.
 */
public final class UserPatchCommand {

    private final String forename;
    private final String surname;
    private final String phoneNumber;
    private final boolean birthDatePresent;
    private final LocalDate birthDate;
    private final String about;
    private final String cbu;
    private final String latestLocale;
    private final String role;
    private final Boolean blocked;
    private final Boolean identityValidated;
    private final Boolean licenseValidated;

    private UserPatchCommand(final Builder builder) {
        this.forename = builder.forename;
        this.surname = builder.surname;
        this.phoneNumber = builder.phoneNumber;
        this.birthDatePresent = builder.birthDatePresent;
        this.birthDate = builder.birthDate;
        this.about = builder.about;
        this.cbu = builder.cbu;
        this.latestLocale = builder.latestLocale;
        this.role = builder.role;
        this.blocked = builder.blocked;
        this.identityValidated = builder.identityValidated;
        this.licenseValidated = builder.licenseValidated;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isBirthDatePresent() {
        return birthDatePresent;
    }

    public String getAbout() {
        return about;
    }

    public String getCbu() {
        return cbu;
    }

    public String getLatestLocale() {
        return latestLocale;
    }

    public String getRole() {
        return role;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public Boolean getIdentityValidated() {
        return identityValidated;
    }

    public Boolean getLicenseValidated() {
        return licenseValidated;
    }

    public boolean hasProfileFields() {
        return forename != null
                || surname != null
                || phoneNumber != null
                || birthDatePresent
                || about != null
                || cbu != null
                || latestLocale != null;
    }

    public boolean hasAdminFields() {
        return role != null
                || blocked != null
                || identityValidated != null
                || licenseValidated != null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String forename;
        private String surname;
        private String phoneNumber;
        private boolean birthDatePresent;
        private LocalDate birthDate;
        private String about;
        private String cbu;
        private String latestLocale;
        private String role;
        private Boolean blocked;
        private Boolean identityValidated;
        private Boolean licenseValidated;

        public Builder forename(final String value) {
            this.forename = value;
            return this;
        }

        public Builder surname(final String value) {
            this.surname = value;
            return this;
        }

        public Builder phoneNumber(final String value) {
            this.phoneNumber = value;
            return this;
        }

        public Builder birthDate(final LocalDate value) {
            this.birthDatePresent = true;
            this.birthDate = value;
            return this;
        }

        public Builder about(final String value) {
            this.about = value;
            return this;
        }

        public Builder cbu(final String value) {
            this.cbu = value;
            return this;
        }

        public Builder latestLocale(final String value) {
            this.latestLocale = value;
            return this;
        }

        public Builder role(final String value) {
            this.role = value;
            return this;
        }

        public Builder blocked(final Boolean value) {
            this.blocked = value;
            return this;
        }

        public Builder identityValidated(final Boolean value) {
            this.identityValidated = value;
            return this;
        }

        public Builder licenseValidated(final Boolean value) {
            this.licenseValidated = value;
            return this;
        }

        public UserPatchCommand build() {
            return new UserPatchCommand(this);
        }
    }
}
