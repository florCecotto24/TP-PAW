package ar.edu.itba.paw.models.domain.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.internal.EntityEquality;
import ar.edu.itba.paw.models.security.UserRole;

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

    /**
     * Credentials epoch embedded in JWTs. Incremented whenever the password hash changes
     * so previously issued access/refresh tokens are rejected after change or reset.
     */
    @Column(name = "password_version", nullable = false)
    private int passwordVersion;

    @Column(name = "email_validated")
    private Boolean emailValidated;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 500)
    private String about;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "profile_picture_id", unique = true)
    private Image profilePicture;

    /**
     * Locale used for async mail copy (Hibernate persists it as a BCP 47 language tag in the
     * {@code latest_locale} VARCHAR column via the built-in {@code LocaleType}). May be {@code null}
     * for legacy rows that never set a preference.
     */
    @Column(name = "latest_locale", length = 32)
    private Locale latestLocale;

    /** Join date (wall calendar); used to show "member since" (month + year) in the UI. */
    @Column(name = "member_since", nullable = false)
    private LocalDate memberSince;

    @Column(length = 22)
    private String cbu;

    @Column(name = "rating_as_rider", precision = 4, scale = 2)
    private BigDecimal ratingAsRider;

    @Column(name = "rating_as_owner", precision = 4, scale = 2)
    private BigDecimal ratingAsOwner;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "license_file_id", unique = true)
    private StoredFile licenseFile;

    @Column(name = "license_validated", nullable = false)
    private boolean licenseValidated;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "identity_file_id", unique = true)
    private StoredFile identityFile;

    @Column(name = "identity_validated", nullable = false)
    private boolean identityValidated;

    @Convert(converter = UserRoleConverter.class)
    @Column(name = "user_role", nullable = false, length = 50)
    private UserRole userRole;

    /**
     * Maps {@link UserRole} to/from the {@code user_role} column. The DB stores
     * {@link UserRole#persistenceName()} (currently uppercase, matching {@link Enum#name()});
     * unknown DB values fall back to {@link UserRole#USER} via {@link UserRole#fromPersistenceName(String)}
     * so authentication keeps working when ops introduces a new role before the enum is updated.
     */
    @Converter
    public static class UserRoleConverter implements AttributeConverter<UserRole, String> {
        @Override
        public String convertToDatabaseColumn(final UserRole attribute) {
            return attribute == null ? null : attribute.persistenceName();
        }

        @Override
        public UserRole convertToEntityAttribute(final String dbData) {
            if (dbData == null) {
                return null;
            }
            return UserRole.fromPersistenceName(dbData).orElse(UserRole.USER);
        }
    }

    /**
     * Admin user id that last assigned {@link #userRole}, stored as a scalar FK.
     * Kept as {@link Long} (not {@code @ManyToOne}) so role history survives if the assigner
     * account is deleted and callers never navigate the association.
     */
    @Column(name = "role_assigned_by")
    private Long roleAssignedBy;

    @Column(nullable = false)
    private boolean blocked;

    /* package */ User() {
        // For Hibernate
    }

    private User(final Builder b) {
        this.id = b.id;
        this.email = b.email;
        this.forename = b.forename;
        this.surname = b.surname;
        this.passwordHash = b.passwordHash;
        this.passwordVersion = b.passwordVersion;
        this.emailValidated = b.emailValidated;
        this.phoneNumber = b.phoneNumber;
        this.birthDate = b.birthDate;
        this.about = b.about;
        this.profilePicture = b.profilePicture;
        this.latestLocale = b.latestLocale;
        this.memberSince = b.memberSince;
        this.cbu = b.cbu;
        this.licenseFile = b.licenseFile;
        this.licenseValidated = b.licenseValidated;
        this.identityFile = b.identityFile;
        this.identityValidated = b.identityValidated;
        this.userRole = b.userRole;
        this.roleAssignedBy = b.roleAssignedBy;
        this.blocked = b.blocked;
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
        private int passwordVersion;
        private Boolean emailValidated;
        private String phoneNumber;
        private LocalDate birthDate;
        private String about;
        private Image profilePicture;
        private Locale latestLocale;
        private LocalDate memberSince;
        private String cbu;
        private StoredFile licenseFile;
        private boolean licenseValidated;
        private StoredFile identityFile;
        private boolean identityValidated;
        private UserRole userRole = UserRole.USER;
        private Long roleAssignedBy = null;
        private boolean blocked = false;

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

        public Builder passwordVersion(final int passwordVersion) {
            this.passwordVersion = passwordVersion;
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

        public Builder profilePicture(final Image profilePicture) {
            this.profilePicture = profilePicture;
            return this;
        }

        public Builder latestLocale(final Locale latestLocale) {
            this.latestLocale = latestLocale;
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

        public Builder licenseFile(final StoredFile licenseFile) {
            this.licenseFile = licenseFile;
            return this;
        }

        public Builder licenseValidated(final boolean licenseValidated) {
            this.licenseValidated = licenseValidated;
            return this;
        }

        public Builder identityFile(final StoredFile identityFile) {
            this.identityFile = identityFile;
            return this;
        }

        public Builder identityValidated(final boolean identityValidated) {
            this.identityValidated = identityValidated;
            return this;
        }

        public Builder userRole(final UserRole userRole) {
            this.userRole = userRole;
            return this;
        }

        public Builder roleAssignedBy(final Long roleAssignedBy) {
            this.roleAssignedBy = roleAssignedBy;
            return this;
        }

        public Builder blocked(final boolean blocked) {
            this.blocked = blocked;
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

    public int getPasswordVersion() {
        return passwordVersion;
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

    public Optional<Image> getProfilePicture() {
        return Optional.ofNullable(profilePicture);
    }

    /** Convenience accessor — returns the profile picture id, or empty if no picture is set. */
    public Optional<Long> getProfilePictureId() {
        return profilePicture == null ? Optional.empty() : Optional.of(profilePicture.getId());
    }

    public Optional<Locale> getLatestLocale() {
        return Optional.ofNullable(latestLocale);
    }

    public Optional<LocalDate> getMemberSince() {
        return Optional.ofNullable(memberSince);
    }

    public Optional<String> getCbu() {
        return Optional.ofNullable(cbu);
    }

    public Optional<StoredFile> getLicenseFile() {
        return Optional.ofNullable(licenseFile);
    }

    /** Convenience accessor — returns the license file id, or empty if no file is set. */
    public Optional<Long> getLicenseFileId() {
        return licenseFile == null ? Optional.empty() : Optional.of(licenseFile.getId());
    }

    public boolean isLicenseValidated() {
        return licenseValidated;
    }

    public Optional<StoredFile> getIdentityFile() {
        return Optional.ofNullable(identityFile);
    }

    /** Convenience accessor — returns the identity file id, or empty if no file is set. */
    public Optional<Long> getIdentityFileId() {
        return identityFile == null ? Optional.empty() : Optional.of(identityFile.getId());
    }

    public boolean isIdentityValidated() {
        return identityValidated;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public Optional<Long> getRoleAssignedBy() {
        return Optional.ofNullable(roleAssignedBy);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public boolean isAdmin() {
        return UserRole.ADMIN == this.userRole;
    }

    public void setUserRole(final UserRole userRole) {
        this.userRole = userRole;
    }

    public void setRoleAssignedBy(final Long roleAssignedBy) {
        this.roleAssignedBy = roleAssignedBy;
    }

    public void setBlocked(final boolean blocked) {
        this.blocked = blocked;
    }

    public void setForename(final String forename) {
        this.forename = forename;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPasswordVersion(final int passwordVersion) {
        this.passwordVersion = passwordVersion;
    }

    public void bumpPasswordVersion() {
        this.passwordVersion++;
    }

    public void setEmailValidated(final Boolean emailValidated) {
        this.emailValidated = emailValidated;
    }

    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setBirthDate(final LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setAbout(final String about) {
        this.about = about;
    }

    public void setProfilePicture(final Image profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setLatestLocale(final Locale latestLocale) {
        this.latestLocale = latestLocale;
    }

    public void setCbu(final String cbu) {
        this.cbu = cbu;
    }

    public void setRatingAsRider(final BigDecimal ratingAsRider) {
        this.ratingAsRider = ratingAsRider;
    }

    public void setRatingAsOwner(final BigDecimal ratingAsOwner) {
        this.ratingAsOwner = ratingAsOwner;
    }

    public void setLicenseFile(final StoredFile licenseFile) {
        this.licenseFile = licenseFile;
    }

    public void setLicenseValidated(final boolean licenseValidated) {
        this.licenseValidated = licenseValidated;
    }

    public void setIdentityFile(final StoredFile identityFile) {
        this.identityFile = identityFile;
    }

    public void setIdentityValidated(final boolean identityValidated) {
        this.identityValidated = identityValidated;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        final User other = (User) o;
        return EntityEquality.equalsByLongId(this, getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }

    @Override
    public String toString() {
        return "User{id=" + id + '}';
    }
}
