package ar.edu.itba.paw.services;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    public Object createUser(final String email){
        return email;
    }
}
