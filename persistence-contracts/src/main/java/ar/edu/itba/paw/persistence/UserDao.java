package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;

import java.util.Optional;

public interface UserDao {

    User createUser(final String email, final String name);

    Optional<User> getUserById(final long id);
}
