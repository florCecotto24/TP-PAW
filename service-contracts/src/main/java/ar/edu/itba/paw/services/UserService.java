package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.User;

public interface UserService {

    /**
     * Step 1 of registration: creates the user with password (BCrypt) and unverified email.
     * @throws ar.edu.itba.paw.exception.user.EmailAlreadyExistsException if the normalized email is already registered
     * @throws ar.edu.itba.paw.exception.user.RegistrationPasswordException on mismatch or too short
     */
    User registerUser(String email, String forename, String surname, String password, String passwordConfirm);

    Optional<User> findByEmail(String email);

    /** Sets {@code email_validated} to true. */
    void markEmailVerified(long userId);

    Optional<User> getUserById(final long id);

    /** Loads {@link User#getPasswordHash()} for Spring Security; empty if unknown email or no password set. */
    Optional<User> findByEmailForAuthentication(final String email);

    /**
     * Role keys as stored in {@code user_roles.role} (same as {@link ar.edu.itba.paw.models.security.UserRole#name()}).
     */
    List<String> findRoleNamesForUser(long userId);

    Optional<User> getListingOwner(final long listingId);

    /**
     * Updates first name and last name (max 50 characters each, no whitespace).
     */
    void updateDisplayName(long userId, String forename, String surname);

    /**
     * Phone: digits and {@code +} only, max 20 chars. Blank input is stored as {@code null}.
     */
    void updatePhoneNumber(long userId, String phoneRaw);

    /**
     * {@code birthDate} may be {@code null} to clear. Must not be after today in {@link ar.edu.itba.paw.models.domain.AvailabilityPeriod#WALL_ZONE}.
     */
    void updateBirthDate(long userId, LocalDate birthDate);

    /**
     * About text shown in profile. Blank input is stored as {@code null}.
     */
    void updateAbout(long userId, String aboutRaw);

    /**
     * Stores a new profile picture and removes the previous image row when present.
     * @throws ar.edu.itba.paw.exception.image.ImageValidationException when payload exceeds configured max size
     */
    void updateProfilePicture(long userId, String originalFilename, String contentType, byte[] data);

    /** Removes the profile picture and deletes the associated image row if it exists. */
    void clearProfilePicture(long userId);

    /**
     * Changes the password of the authenticated user validating the current one.
     * @throws ar.edu.itba.paw.exception.user.IncorrectCurrentPasswordException if the current password does not match
     */
    void changePassword(long userId, String currentPassword, String newPassword, String newPasswordConfirm);

    /**
         * Usuario legacy sin {@code password_hash}: genera una contraseña aleatoria, la persiste hasheada y la envía por correo en texto plano.
         * Si el usuario ya tiene contraseña, no hace nada (idempotente).
     */
    void assignRandomPasswordAndEmailForLegacyUser(long userId, String email, Locale locale);

    /** Persists the user's preferred UI/mail locale tag (BCP 47, truncated to 32 chars). */
    void updateLatestLocale(long userId, String localeTag);

    /** Locale for async mail copy for this user; defaults to English when unknown. */
    Locale resolveMailLocale(long userId);

    /**
     * Locale for mail when the user has a persisted {@link User#getLatestLocaleTag()}; otherwise {@code fallback}
     * (or English if {@code fallback} is null).
     */
    Locale resolveMailLocaleOrElse(long userId, Locale fallback);
}
