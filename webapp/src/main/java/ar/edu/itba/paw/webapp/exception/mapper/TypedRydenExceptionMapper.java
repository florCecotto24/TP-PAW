package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/**
 * Base {@link ExceptionMapper} for a single {@link RydenException} subtype with a fixed HTTP status.
 */
abstract class TypedRydenExceptionMapper<E extends RydenException> implements ExceptionMapper<E> {

    private final LocaleMessages localeMessages;
    private final Response.Status status;

    TypedRydenExceptionMapper(final LocaleMessages localeMessages, final Response.Status status) {
        this.localeMessages = localeMessages;
        this.status = status;
    }

    @Override
    public final Response toResponse(final E exception) {
        return RydenExceptionResponseSupport.toResponse(exception, status, localeMessages);
    }
}
