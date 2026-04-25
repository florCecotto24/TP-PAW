package ar.edu.itba.paw.models;

import java.time.LocalDate;
import java.util.Optional;

public class User {
    private final long id;
    private final String email;
    private final String forename;
    private final String surname;
    private final String passwordHash;
    private final Boolean emailValidated;
    private final String phoneNumber;
    private final LocalDate birthDate;
    private final Long profilePictureId;
    /** BCP 47 tag (e.g. {@code en}, {@code es}) for async mail copy; may be null for legacy rows. */
    private final String latestLocaleTag;

    public User(final long id, final String email, final String forename, final String surname) {
        this(id, email, forename, surname, null, null, null, null, null, null);
    }

    public User(final long id, final String email, final String forename, final String surname, final String passwordHash) {
        this(id, email, forename, surname, passwordHash, null, null, null, null, null);
    }

    public User(
            final long id,
            final String email,
            final String forename,
            final String surname,
            final String passwordHash,
            final Boolean emailValidated,
            final String phoneNumber,
            final LocalDate birthDate) {
        this(id, email, forename, surname, passwordHash, emailValidated, phoneNumber, birthDate, null, null);
    }

    public User(
            final long id,
            final String email,
            final String forename,
            final String surname,
            final String passwordHash,
            final Boolean emailValidated,
            final String phoneNumber,
            final LocalDate birthDate,
            final Long profilePictureId) {
        this(id, email, forename, surname, passwordHash, emailValidated, phoneNumber, birthDate, profilePictureId, null);
    }

    public User(
            final long id,
            final String email,
            final String forename,
            final String surname,
            final String passwordHash,
            final Boolean emailValidated,
            final String phoneNumber,
            final LocalDate birthDate,
            final Long profilePictureId,
            final String latestLocaleTag) {
        this.id = id;
        this.email = email;
        this.forename = forename;
        this.surname = surname;
        this.passwordHash = passwordHash;
        this.emailValidated = emailValidated;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.profilePictureId = profilePictureId;
        this.latestLocaleTag = latestLocaleTag;
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

    public Optional<Long> getProfilePictureId() {
        return Optional.ofNullable(profilePictureId);
    }

    public Optional<String> getLatestLocaleTag() {
        return Optional.ofNullable(latestLocaleTag).filter(s -> !s.isBlank());
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
