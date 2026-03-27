package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public Optional<User> getUserById(final long id){
        return userDao.getUserById(id);
    }
}
