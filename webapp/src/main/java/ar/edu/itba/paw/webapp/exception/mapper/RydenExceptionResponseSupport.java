package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.webapp.dto.rest.ErrorDto;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/** Builds vendor {@link ErrorDto} responses for domain {@link RydenException} subtypes. */
public final class RydenExceptionResponseSupport {

    private RydenExceptionResponseSupport() {
    }

    public static Response toResponse(
            final RydenException exception,
            final Response.Status status,
            final LocaleMessages localeMessages) {
        final ErrorDto body = ErrorDto.of(
                status.getStatusCode(),
                exception.getMessageCode(),
                localeMessages.msg(exception));
        return Response.status(status)
                .type(ErrorDto.mediaType())
                .entity(body)
                .build();
    }
}
