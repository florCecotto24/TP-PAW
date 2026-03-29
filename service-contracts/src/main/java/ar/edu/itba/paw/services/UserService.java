package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;

import java.util.Optional;

public interface UserService {

    User createUser(final String email, final String name);

    Optional<User> getUserById(final long id);

    /** Reutiliza el usuario si ya existe el mismo email; si no, lo crea. */
    User getOrCreateUser(final String email, final String name);
}
