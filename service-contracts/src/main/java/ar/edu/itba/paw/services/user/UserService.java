package ar.edu.itba.paw.services.user;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.domain.user.UserDocumentType;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.file.BinaryContent;
import ar.edu.itba.paw.models.dto.profile.ProfileUpdateRequest;
import ar.edu.itba.paw.models.security.UserRole;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
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
     * Roles assigned to {@code userId}, decoded as the typed enum. Auth-tier callers should use
     * {@link ar.edu.itba.paw.webapp.security.auth.userdetails.UserRoleAuthorities#fromUserRoles}
     * (or equivalent) to map each role to its Spring authority — the previous {@code List<String>}
     * shape forced every caller to re-parse strings into roles.
     */
    List<UserRole> findRolesForUser(long userId);

    /**
     * Updates first name and last name (max 50 characters each, no whitespace).
     */
    void updateDisplayName(long userId, String forename, String surname);

    /**
     * Phone: digits and {@code +} only, max 20 chars. Blank input is stored as {@code null}.
     */
    void updatePhoneNumber(long userId, String phoneRaw);

    /**
     * {@code birthDate} may be {@code null} to clear. Must not be after today in {@link ar.edu.itba.paw.models.util.time.AppTimezone#WALL_ZONE}.
     */
    void updateBirthDate(long userId, LocalDate birthDate);

    /**
     * About text shown in profile. Blank input is stored as {@code null}.
     */
    void updateAbout(long userId, String aboutRaw);

    /**
     * Atomically applies every "edit profile" field in a single transaction. Use this from the
     * profile form handler instead of calling each {@code updateXxx} sequentially, otherwise a
     * mid-flight failure leaves the profile in a half-updated state.
     *
     * @throws ar.edu.itba.paw.exception.RydenException when any field fails validation; the whole
     *         operation rolls back.
     */
    void updateProfile(long userId, ProfileUpdateRequest request);

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

    /** Persists the user's average rating as rider; {@code null} clears the value. */
    void updateRatingAsRider(long userId, BigDecimal average);

    /** Persists the user's average rating as owner; {@code null} clears the value. */
    void updateRatingAsOwner(long userId, BigDecimal average);

    /**
     * The user's explicitly-chosen UI locale (from {@code latest_locale}), if any. Returns {@link Optional#empty()}
     * when the user has not picked one; callers can then fall back to a cookie or system default. Only returns
     * locales the application supports; unknown stored tags are normalised to empty.
     */
    Optional<Locale> findUserPreferredLocale(long userId);

    /** Locale for async mail copy for this user; defaults to English when unknown. */
    Locale resolveMailLocale(long userId);

    /**
     * Locale for mail when the user has a persisted {@link User#getLatestLocale()}; otherwise {@code fallback}
     * (or English if {@code fallback} is null).
     */
    Locale resolveMailLocaleOrElse(long userId, Locale fallback);

    /**
     * In-memory variant of {@link #resolveMailLocale(long)} for callers that already hold a loaded
     * {@link User} entity (e.g. schedulers that JOIN FETCH rider/owner). Avoids a redundant
     * {@code SELECT users WHERE id = ?} per row in tight loops.
     */
    Locale resolveMailLocaleFor(User user);

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

    /** Owner CBU for the given car, when present and non-blank. */
    Optional<String> findOwnerCbuForCar(long carId);

    /** Whether {@code user} has a persisted CBU that satisfies {@link ar.edu.itba.paw.models.util.rules.CbuRules}. */
    boolean hasValidCbu(User user);

    /**
     * Whether both profile verification documents are present (license and government ID file slots).
     * Used to gate booking when riders must upload documentation first.
     */
    boolean hasUploadedLicenseAndIdentity(User user);

    /**
     * Combined gate used by the publish-car flow: the user has a valid CBU <em>and</em> a stored
     * identity document. Centralises what the controller previously assembled inline as
     * {@code hasValidCbu(user) && user.getIdentityFileId().isPresent()} so callers do not need
     * to remember which fields participate in the prerequisite.
     */
    boolean meetsPublishingPrerequisites(User user);

    /**
     * Resolves the stored profile document of {@code documentType} for {@code userId} in one call.
     * Returns empty when the user is unknown, the slot is empty, or the underlying stored file row
     * is missing. Centralises the two-step lookup ({@link #getUserById(long)} plus the per-document
     * file id) so the download/view endpoints don't have to.
     */
    Optional<StoredFile> findProfileDocument(long userId, UserDocumentType documentType);

    /**
     * Same scoping as {@link #findProfileDocument} but returns a detached
     * {@link BinaryContent} value object so download endpoints don't leak the JPA entity.
     */
    Optional<BinaryContent> findProfileDocumentContent(long userId, UserDocumentType documentType);

    /** Whether {@code cbuRaw} is acceptable as a CBU per {@link ar.edu.itba.paw.models.util.rules.CbuRules}. */
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

    /**
     * Marks {@code userId} as blocked. Intended for system-driven blocks (e.g. refund-proof overdue sweep);
     * admin-initiated blocks live in {@link AdminService#blockUser(long, long)} and include extra validations.
     */
    void blockUser(long userId);

    /**
     * Clears the {@code blocked} flag for {@code userId}. Idempotent — a no-op when the user is already unblocked.
     */
    void unblockUser(long userId);

    // -----------------------------------------------------------------------------------------------------------
    // Admin-orchestrated operations on user rows.
    //
    // These methods exist so that {@link AdminService} can mutate user state without bypassing the layering rule
    // "each service may only call its own DAO". They are intentionally narrow and do NOT include the admin-side
    // policy checks (self-block, grantor-block, etc.); the calling {@link AdminService} owns those.
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Promotes {@code targetUserId} to admin role, recording {@code assignedByUserId} as the granting admin.
     * Sends a notification email to the promoted user mentioning the granting admin's name.
     *
     * @throws ar.edu.itba.paw.exception.user.UserNotFoundException when either the granting admin
     *         or the target user does not exist
     * @throws ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException when
     *         {@code assignedByUserId} is not an administrator
     * @throws ar.edu.itba.paw.exception.admin.UserAlreadyAdminException when
     *         {@code targetUserId} is already an administrator
     */
    void promoteToAdmin(long targetUserId, long assignedByUserId);

    /**
     * Creates a user row directly from a pre-encoded password hash (BCrypt). Bypasses the registration form
     * validation and is intended for admin-initiated account creation only.
     */
    User createUserWithEncodedPassword(String email, String forename, String surname, String bcryptEncodedHash);

    /**
     * Creates a user row with admin role, a pre-encoded password hash, and the email pre-verified.
     * {@code assignedByUserId} is recorded as the granting admin. Intended for admin-initiated account creation.
     *
     * @throws ar.edu.itba.paw.exception.user.UserNotFoundException when {@code assignedByUserId} does not exist
     * @throws ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException when {@code assignedByUserId} is not an administrator
     */
    User createAdminUserWithEncodedPassword(String email, String forename, String surname,
                                            String bcryptEncodedHash, long assignedByUserId);

    /**
     * Paginated list of every user in the system, ordered by id. Admin-only listing.
     */
    Page<User> findAllUsersPaginated(int page, int pageSize);

    // -----------------------------------------------------------------------------------------------------------
    // Profile-media-orchestrated operations on user rows.
    //
    // These methods exist so that {@link UserProfileMediaService} can mutate user-row FKs (profile picture,
    // KYC document files) without bypassing the layering rule "each service may only call its own DAO".
    // They are intentionally narrow row-level setters with no validation or side effects beyond the FK update;
    // the calling {@link UserProfileMediaService} owns file-size, content-type, and slot-already-occupied gates.
    // -----------------------------------------------------------------------------------------------------------

    /**
     * Sets the user's {@code profile_picture_id} FK. Pass {@code null} to clear. No-op when {@code userId} is unknown.
     */
    void updateProfilePictureFk(long userId, Long imageId);

    /**
     * Sets the user's {@code license_file_id} FK and the {@code license_validated} flag in one row update.
     */
    void updateLicenseDocumentFk(long userId, long storedFileId, boolean validated);

    /**
     * Sets the user's {@code identity_file_id} FK and the {@code identity_validated} flag in one row update.
     */
    void updateIdentityDocumentFk(long userId, long storedFileId, boolean validated);

    /**
     * Clears the user's {@code license_file_id} FK and resets {@code license_validated} to {@code false}.
     */
    void clearLicenseDocumentFk(long userId);

    /**
     * Clears the user's {@code identity_file_id} FK and resets {@code identity_validated} to {@code false}.
     */
    void clearIdentityDocumentFk(long userId);
}
