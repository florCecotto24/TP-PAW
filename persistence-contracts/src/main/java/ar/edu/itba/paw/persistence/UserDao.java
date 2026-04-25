package ar.edu.itba.paw.persistence;

import java.time.LocalDate;
import java.util.Optional;

import ar.edu.itba.paw.models.User;

public interface UserDao {
    User createUser(String email, String forename, String surname, String passwordHash);

    default User createUser(final String email, final String forename, final String surname) {
        return createUser(email, forename, surname, null);
    }

    Optional<User> getUserById(final long id);

    Optional<User> findByEmail(final String email);

    Optional<User> findByEmailForAuthentication(final String email);

    void updateUserName(long userId, String forename, String surname);

    void updatePhoneNumber(long userId, String phoneNumber);

    void updateBirthDate(long userId, LocalDate birthDate);

    void updateProfilePictureId(long userId, Long profilePictureImageId);

    void updateEmailValidated(long userId, boolean validated);

    void updatePasswordHash(long userId, String passwordHash);

    Optional<User> getListingOwner(final long listingId);

    void updateLatestLocale(long userId, String localeTag);
}
