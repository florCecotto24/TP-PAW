package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;

import java.util.Optional;

public interface UserService {

    User createUser(final String email, final String name);

    Optional<User> getUserById(final long id);

}
