package ar.edu.itba.paw.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;

/** Users, roles, profile fields, and listing-owner resolution joins. */
public interface UserDao {
    User createUser(String email, String forename, String surname, String passwordHash);

    default User createUser(final String email, final String forename, final String surname) {
        return createUser(email, forename, surname, null);
    }

    /**
     * Creates a user row with the admin role, the email pre-verified, and {@code assignedByUserId}
     * recorded as the granting admin. Intended for admin-initiated account creation only.
     */
    User createAdminUser(String email, String forename, String surname,
                         String passwordHash, Long assignedByUserId);

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

    void updatePasswordHash(long userId, String passwordHash);

    void updateLatestLocale(long userId, String localeTag);

    /** Persists the user's average rating as rider; {@code null} clears the value. */
    void updateRatingAsRider(long userId, java.math.BigDecimal average);

    /** Persists the user's average rating as owner; {@code null} clears the value. */
    void updateRatingAsOwner(long userId, java.math.BigDecimal average);

    List<String> findRoleNamesForUser(long userId);

    void promoteToAdmin(long userId, Long assignedByUserId);

    void blockUser(long userId);

    void unblockUser(long userId);

    Page<User> findAllUsersPaginated(int page, int pageSize);

    void updateCbu(final long userId, final String cbu);
}
