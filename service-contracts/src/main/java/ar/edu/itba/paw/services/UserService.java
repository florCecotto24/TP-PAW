package ar.edu.itba.paw.services;

import java.util.Optional;

import ar.edu.itba.paw.models.User;

public interface UserService {

    /** @throws EmailAlreadyExistsException if the normalized email is already registered */
    User createUser(final String email, final String forename, final String surname);

    Optional<User> getUserById(final long id);

    User findOrCreatePublisher(final String email, final String forename, final String surname);
}
