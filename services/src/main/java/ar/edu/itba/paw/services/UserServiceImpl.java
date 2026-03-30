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
    public User createUser(final String email, final String name){
        return userDao.createUser(email, name);
    }

    @Override
    public Optional<User> getUserById(final long id) {
        return userDao.getUserById(id);
    }

    @Override
    public User findOrCreatePublisher(final String email, final String name) {
        final String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        final String trimmedName = name.trim();
        final Optional<User> existing = userDao.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            final User u = existing.get();
            userDao.updateUserName(u.getId(), trimmedName);
            return new User(u.getId(), u.getEmail(), trimmedName);
        }
        return userDao.createUser(normalizedEmail, trimmedName);
    }
}
