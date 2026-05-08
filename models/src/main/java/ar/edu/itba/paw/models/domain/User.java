package ar.edu.itba.paw.models.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Domain user. Prefer {@link #builder()} or {@link #identities(long, String, String, String)} over ad-hoc construction.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_id_seq")
    @SequenceGenerator(name = "users_id_seq", sequenceName = "users_id_seq", allocationSize = 1)
    private long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 50)
    private String forename;

    @Column(nullable = false, length = 50)
    private String surname;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "email_validated")
    private Boolean emailValidated;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 500)
    private String about;

    @Column(name = "profile_picture_id")
    private Long profilePictureId;

    /** BCP 47 tag (e.g. {@code en}, {@code es}) for async mail copy; may be null for legacy rows. */
    @Column(name = "latest_locale", length = 32)
    private String latestLocaleTag;

    /** Join date (wall calendar); used to show "member since" (month + year) in the UI. */
    @Column(name = "member_since", nullable = false)
    private LocalDate memberSince;

    @Column(length = 22)
    private String cbu;

    @Column(name = "license_file_id")
    private Long licenseFileId;

    @Column(name = "license_validated")
    private Boolean licenseValidated;

    @Column(name = "identity_file_id")
    private Long identityFileId;

    @Column(name = "identity_validated")
    private Boolean identityValidated;

    /* package */ User() {
        // For Hibernate
    }

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
