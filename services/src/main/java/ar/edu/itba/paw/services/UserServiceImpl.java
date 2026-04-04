package ar.edu.itba.paw.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ar.edu.itba.paw.models.EmailNormalizer;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;

@Service
public class UserServiceImpl implements UserService {
    private final UserDao userDao;

    @Autowired
    public UserServiceImpl(final UserDao userDao){
        this.userDao = userDao;
    }

    @Override
    public User createUser(final String email, final String forename, final String surname) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyExistsException("A user with this email already exists");
        }
        return userDao.createUser(normalizedEmail, forename, surname);
    }

    @Override
    public Optional<User> getUserById(final long id) {
        return userDao.getUserById(id);
    }

    // to be deleted when real users with spring security are implemented
    @Override
    public User findOrCreatePublisher(final String email, final String forename, final String surname) {
        final String normalizedEmail = EmailNormalizer.normalize(email);
        final Optional<User> existing = userDao.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            final User u = existing.get();
            userDao.updateUserName(u.getId(), forename, surname);
            return new User(u.getId(), u.getEmail(), forename, surname);
        }
        return userDao.createUser(normalizedEmail, forename, surname);
    }
}
