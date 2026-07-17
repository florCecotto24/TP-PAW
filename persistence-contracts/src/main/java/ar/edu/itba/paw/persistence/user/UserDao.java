package ar.edu.itba.paw.persistence.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.security.UserRole;

/** Users, roles, profile fields, and listing-owner resolution joins. */
public interface UserDao {
    /**
     * Persists a new user row with the role / verification flag / grantor decided by the caller.
     * The DAO does not pick defaults for these values: the service tier is the single source of
     * truth for "what kind of account is this?". Standard self-registration passes
     * {@link UserRole#USER}, {@code emailValidated=false} and {@code assignedByUserId=null};
     * admin-initiated account creation passes {@link UserRole#ADMIN}, {@code emailValidated=true}
     * and the granting admin's id.
     */
    User createUser(String email, String forename, String surname, String passwordHash,
                    UserRole role, boolean emailValidated, Long assignedByUserId);

    /** Convenience self-registration overload: USER role, unverified email, no grantor. */
    default User createUser(final String email, final String forename, final String surname, final String passwordHash) {
        return createUser(email, forename, surname, passwordHash, UserRole.USER, false, null);
    }

    /** Convenience self-registration overload with no password set yet. */
    default User createUser(final String email, final String forename, final String surname) {
        return createUser(email, forename, surname, null, UserRole.USER, false, null);
    }

    Optional<User> getUserById(final long id);

    Optional<User> findByEmail(final String email);

    Optional<User> findByEmailForAuthentication(final String email);

    void updateUserName(long userId, String forename, String surname);

    void updatePhoneNumber(long userId, String phoneNumber);

    void updateBirthDate(long userId, LocalDate birthDate);

    void updateAbout(long userId, String about);

    void updateProfilePictureId(long userId, Long profilePictureImageId);

    void updateLicenseDocument(long userId, long fileId, boolean validated);
    void clearLicenseDocument(long userId);

    void updateIdentityDocument(long userId, long fileId, boolean validated);
    void clearIdentityDocument(long userId);

    void updateEmailValidated(long userId, boolean validated);

    /** Replaces the password hash and bumps {@code password_version} (credentials epoch). */
    void updatePasswordHash(long userId, String passwordHash);

    void updateLatestLocale(long userId, String localeTag);

    /** Persists the user's average rating as rider; {@code null} clears the value. */
    void updateRatingAsRider(long userId, java.math.BigDecimal average);

    /** Persists the user's average rating as owner; {@code null} clears the value. */
    void updateRatingAsOwner(long userId, java.math.BigDecimal average);

    /**
     * Roles assigned to {@code userId}, decoded as the typed enum so callers don't have to
     * re-parse strings at the auth tier.
     */
    List<UserRole> findRolesForUser(long userId);

    /**
     * Sets {@code userId}'s role and records {@code assignedByUserId} as the granting admin. The
     * service tier decides which role to assign; the DAO just persists.
     */
    void updateUserRoleAndGrantor(long userId, UserRole role, Long assignedByUserId);

    void blockUser(long userId);

    void unblockUser(long userId);

    Page<User> findAllUsersPaginated(int page, int pageSize);

    /**
     * Admin user listing with optional filters. {@code role} and {@code query} may be {@code null}
     * to skip that filter; {@code blocked} is tri-state ({@code null} = any).
     */
    Page<User> findUsersPaginated(int page, int pageSize, Boolean blocked, UserRole role, String query);

    void updateIdentityValidated(long userId, boolean validated);

    void updateLicenseValidated(long userId, boolean validated);

    void updateCbu(final long userId, final String cbu);

    /** Permanently removes the user row (DB cascades related data). */
    void deleteUser(long userId);
}
