package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    private final UserDao userDao;

    @Autowired
    public UserServiceImpl(final UserDao userDao){
        this.userDao = userDao;
    }

    @Override
    public User createUser(final String email, final String forename, final String surname) {
        return userDao.createUser(email, forename, surname);
    }

    @Override
    public Optional<User> getUserById(final long id) {
        return userDao.getUserById(id);
    }

    @Override
    public User findOrCreatePublisher(final String email, final String forename, final String surname) {
        final String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        final Optional<User> existing = userDao.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            final User u = existing.get();
            userDao.updateUserName(u.getId(), forename, surname);
            return new User(u.getId(), u.getEmail(), forename, surname);
        }
        return userDao.createUser(normalizedEmail, forename, surname);
    }
}
