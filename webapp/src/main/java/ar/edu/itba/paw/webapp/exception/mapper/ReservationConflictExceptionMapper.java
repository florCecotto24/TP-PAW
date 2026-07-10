package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@Provider
@Component
public final class ReservationConflictExceptionMapper extends TypedRydenExceptionMapper<ReservationConflictException> {

    @Autowired
    public ReservationConflictExceptionMapper(final LocaleMessages localeMessages) {
        super(localeMessages, Response.Status.CONFLICT);
    }
}
