package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.domain.UserDocumentType;

/**
 * Account, profile, credentials, CBU, and user rows needed elsewhere (e.g. listing owner resolution for mail/UI).
 * Implementations use {@code UserDao} only for user rows; files and mail go through {@code StoredFileService}, {@code ImageService}, {@code EmailService}, and related helpers.
 */
public interface UserService {

    /**
     * Step 1 of registration: creates the user with password (BCrypt) and unverified email.
     * @throws ar.edu.itba.paw.exception.user.EmailAlreadyExistsException if the normalized email is already registered
     * @throws ar.edu.itba.paw.exception.user.RegistrationPasswordException on mismatch or too short
     */
    User registerUser(String email, String forename, String surname, String password, String passwordConfirm);

    /** Lookup by normalized email; empty when not registered. */
    Optional<User> findByEmail(String email);

    /** Sets {@code email_validated} to true. */
    void markEmailVerified(long userId);

    /** Loads a user row by primary key when present. */
    Optional<User> getUserById(final long id);

    /** Loads {@link User#getPasswordHash()} for Spring Security; empty if unknown email or no password set. */
    Optional<User> findByEmailForAuthentication(final String email);

    /**
     * Role keys as stored in {@code user_roles.role} (same as {@link ar.edu.itba.paw.models.security.UserRole#name()}).
     */
    List<String> findRoleNamesForUser(long userId);

    /** User who owns the car linked to {@code listingId}, when the join resolves. */
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
     * Stores one profile document per {@link UserDocumentType}. If that slot already has a file, remove it first via
     * {@link #clearProfileDocument(long, UserDocumentType)}.
     */
    void uploadValidatedProfileDocument(long userId, UserDocumentType documentType, String originalFilename, String contentType, byte[] data);

    /** Clears the stored file reference for the given document slot. */
    void clearProfileDocument(long userId, UserDocumentType documentType);

    /**
     * Changes the password of the authenticated user validating the current one.
     * @throws ar.edu.itba.paw.exception.user.IncorrectCurrentPasswordException if the current password does not match
     */
    void changePassword(long userId, String currentPassword, String newPassword, String newPasswordConfirm);

    /**
     * Replaces {@code password_hash} with {@code bcryptEncodedHash} (caller supplies an already-encoded secret, e.g.
     * from {@code PasswordEncoder}). Used after password-reset code validation.
     *
     * @throws ar.edu.itba.paw.exception.user.UserNotFoundException when {@code userId} is unknown
     */
    void replacePasswordHash(long userId, String bcryptEncodedHash);

    /**
     * Legacy user without {@code password_hash}: generate a random password, persist the hash, and email the plain text once.
     * If the user already has a password, no-op (idempotent).
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

    /**
     * Persists a valid CBU or clears it when the trimmed input is empty.
     * @throws ar.edu.itba.paw.exception.user.InvalidCbuFormatException when input is non-blank but not a valid CBU
     */
    void updateCbu(long userId, String cbu);

    /**
     * Non-blank CBU for payouts; {@code userId} must exist and have a stored CBU.
     *
     * @throws ar.edu.itba.paw.exception.user.UserNotFoundException when the account is missing
     * @throws ar.edu.itba.paw.exception.user.CBUNotFoundException when CBU is unset or blank
     */
    String getUserCbu(long userId);

    /**
     * Resolves the listing owner's CBU for read-only UI (e.g. rider reservation confirmation).
     *
     * @return non-empty when the listing owner exists and has a non-blank CBU; otherwise empty
     */
    Optional<String> findOwnerCbuForListing(long listingId);

    /** Whether {@code user} has a persisted CBU that satisfies {@link ar.edu.itba.paw.models.util.CbuRules}. */
    boolean hasValidCbu(User user);

    /**
     * Whether both profile verification documents are present (license and government ID file slots).
     * Used to gate booking when riders must upload documentation first.
     */
    boolean hasUploadedLicenseAndIdentity(User user);

    /** Whether {@code cbuRaw} is acceptable as a CBU per {@link ar.edu.itba.paw.models.util.CbuRules}. */
    boolean isValidCbuFormat(String cbuRaw);

    /**
     * Registration: creates the user and runs the post-registration account-confirmation step (policy and side effects are internal to the service).
     */
    User registerUserRequiringAccountConfirmation(
            String email, String forename, String surname, String password, String passwordConfirm, Locale locale);

    /**
     * When the account-confirmation screen is shown, ensures prerequisites so the user can complete the step (no-op if {@code email} is blank or unknown).
     */
    void ensureAccountConfirmationPrerequisites(String email, Locale locale);

    /**
     * Asks the service to issue another confirmation for {@code email}.
     * @return {@code false} if there is no account for that address (caller may show a generic message)
     */
    boolean requestAccountConfirmationResend(String email, Locale locale);

    /** Confirms the account with the code the user entered; returns the user id. */
    long completeAccountConfirmation(String email, String code);
}
