package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;

import java.util.Optional;

public interface UserDao {
    User createUser(String email, String forename, String surname);

    Optional<User> getUserById(final long id);

    Optional<User> findByEmail(final String email);

    void updateUserName(long userId, String forename, String surname);

    Optional<User> getListingOwner(final long listingId);
}
