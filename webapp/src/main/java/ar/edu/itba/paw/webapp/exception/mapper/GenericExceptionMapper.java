package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.webapp.dto.rest.ErrorDto;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/** Last-resort mapper for unexpected failures — never exposes raw {@code Throwable#getMessage()}. */
@Provider
@Component
public final class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericExceptionMapper.class);

    private final LocaleMessages localeMessages;

    @Autowired
    public GenericExceptionMapper(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @Override
    public Response toResponse(final Exception exception) {
        LOGGER.atError().setCause(exception).log("Unhandled exception");
        final String code = MessageKeys.ERROR_UNEXPECTED;
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(ErrorDto.mediaType())
                .entity(ErrorDto.of(
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        code,
                        localeMessages.msg(code)))
                .build();
    }
}
