package ar.edu.itba.paw.models.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain user. Prefer {@link #builder()} or {@link #identities(long, String, String, String)} over ad-hoc construction.
 */
public final class User {

    private final long id;
    private final String email;
    private final String forename;
    private final String surname;
    private final String passwordHash;
    private final Boolean emailValidated;
    private final String phoneNumber;
    private final LocalDate birthDate;
    private final String about;
    private final Long profilePictureId;
    /** BCP 47 tag (e.g. {@code en}, {@code es}) for async mail copy; may be null for legacy rows. */
    private final String latestLocaleTag;
    /** Join date (wall calendar); used to show "member since" (month + year) in the UI. */
    private final LocalDate memberSince;
    private final String cbu;
    private final Long licenseFileId;
    private final Boolean licenseValidated;
    private final Long insuranceFileId;
    private final Boolean insuranceValidated;
    private final Long identityFileId;
    private final Boolean identityValidated;

    private User(final Builder b) {
        this.id = b.id;
        this.email = b.email;
        this.forename = b.forename;
        this.surname = b.surname;
        this.passwordHash = b.passwordHash;
        this.emailValidated = b.emailValidated;
        this.phoneNumber = b.phoneNumber;
        this.birthDate = b.birthDate;
        this.about = b.about;
        this.profilePictureId = b.profilePictureId;
        this.latestLocaleTag = b.latestLocaleTag;
        this.memberSince = b.memberSince;
        this.cbu = b.cbu;
        this.licenseFileId = b.licenseFileId;
        this.licenseValidated = b.licenseValidated;
        this.insuranceFileId = b.insuranceFileId;
        this.insuranceValidated = b.insuranceValidated;
        this.identityFileId = b.identityFileId;
        this.identityValidated = b.identityValidated;
    }

    /** Minimal user row (no password hash, no optional profile fields). */
    public static User identities(final long id, final String email, final String forename, final String surname) {
        return builder().id(id).email(email).forename(forename).surname(surname).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long id;
        private String email;
        private String forename;
        private String surname;
        private String passwordHash;
        private Boolean emailValidated;
        private String phoneNumber;
        private LocalDate birthDate;
        private String about;
        private Long profilePictureId;
        private String latestLocaleTag;
        private LocalDate memberSince;
        private String cbu;
        private Long licenseFileId;
        private Boolean licenseValidated;
        private Long insuranceFileId;
        private Boolean insuranceValidated;
        private Long identityFileId;
        private Boolean identityValidated;

        public Builder id(final long id) {
            this.id = id;
            return this;
        }

        public Builder email(final String email) {
            this.email = email;
            return this;
        }

        public Builder forename(final String forename) {
            this.forename = forename;
            return this;
        }

        public Builder surname(final String surname) {
            this.surname = surname;
            return this;
        }

        public Builder passwordHash(final String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder emailValidated(final Boolean emailValidated) {
            this.emailValidated = emailValidated;
            return this;
        }

        public Builder phoneNumber(final String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder birthDate(final LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder about(final String about) {
            this.about = about;
            return this;
        }

        public Builder profilePictureId(final Long profilePictureId) {
            this.profilePictureId = profilePictureId;
            return this;
        }

        public Builder latestLocaleTag(final String latestLocaleTag) {
            this.latestLocaleTag = latestLocaleTag;
            return this;
        }

        public Builder memberSince(final LocalDate memberSince) {
            this.memberSince = memberSince;
            return this;
        }

        public Builder cbu(final String cbu) {
            this.cbu = cbu;
            return this;
        }

        public Builder licenseFileId(final Long licenseFileId) {
            this.licenseFileId = licenseFileId;
            return this;
        }

        public Builder licenseValidated(final Boolean licenseValidated) {
            this.licenseValidated = licenseValidated;
            return this;
        }

        public Builder insuranceFileId(final Long insuranceFileId) {
            this.insuranceFileId = insuranceFileId;
            return this;
        }

        public Builder insuranceValidated(final Boolean insuranceValidated) {
            this.insuranceValidated = insuranceValidated;
            return this;
        }

        public Builder identityFileId(final Long identityFileId) {
            this.identityFileId = identityFileId;
            return this;
        }

        public Builder identityValidated(final Boolean identityValidated) {
            this.identityValidated = identityValidated;
            return this;
        }

        public User build() {
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(forename, "forename");
            Objects.requireNonNull(surname, "surname");
            return new User(this);
        }
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    public Optional<String> getPasswordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public Optional<Boolean> getEmailValidated() {
        return Optional.ofNullable(emailValidated);
    }

    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public Optional<LocalDate> getBirthDate() {
        return Optional.ofNullable(birthDate);
    }

    public Optional<String> getAbout() {
        return Optional.ofNullable(about);
    }

    public Optional<Long> getProfilePictureId() {
        return Optional.ofNullable(profilePictureId);
    }

    public Optional<String> getLatestLocaleTag() {
        return Optional.ofNullable(latestLocaleTag).filter(s -> !s.isBlank());
    }

    public Optional<LocalDate> getMemberSince() {
        return Optional.ofNullable(memberSince);
    }

    public Optional<String> getCbu() {
        return Optional.ofNullable(cbu);
    }

    public Optional<Long> getLicenseFileId() {
        return Optional.ofNullable(licenseFileId);
    }

    public boolean isLicenseValidated() {
        return Boolean.TRUE.equals(licenseValidated);
    }

    public Optional<Long> getInsuranceFileId() {
        return Optional.ofNullable(insuranceFileId);
    }

    public boolean isInsuranceValidated() {
        return Boolean.TRUE.equals(insuranceValidated);
    }

    public Optional<Long> getIdentityFileId() {
        return Optional.ofNullable(identityFileId);
    }

    public boolean isIdentityValidated() {
        return Boolean.TRUE.equals(identityValidated);
    }

    /**
     * Entity identity based on persisted {@code id}. Two instances with {@code id == 0} are not considered equal
     * unless they are the same object reference.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        final User other = (User) o;
        if (id == 0L || other.id == 0L) {
            return false;
        }
        return id == other.id;
    }

    @Override
    public int hashCode() {
        if (id == 0L) {
            return System.identityHashCode(this);
        }
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", forename='" + forename + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }
}
