package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@Provider
@Component
public final class UserNotFoundExceptionMapper extends TypedRydenExceptionMapper<UserNotFoundException> {

    @Autowired
    public UserNotFoundExceptionMapper(final LocaleMessages localeMessages) {
        super(localeMessages, Response.Status.NOT_FOUND);
    }
}
