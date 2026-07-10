package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.dto.rest.ErrorDto;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/**
 * Maps JAX-RS {@link NotFoundException} to {@code error.v1+json} before the broader
 * {@link WebApplicationExceptionMapper} catch-all.
 */
@Provider
@Component
public final class JaxRsNotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRsNotFoundExceptionMapper.class);

    private final LocaleMessages localeMessages;

    @Autowired
    public JaxRsNotFoundExceptionMapper(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @Override
    public Response toResponse(final NotFoundException exception) {
        LOGGER.atDebug().addArgument(exception.getMessage()).log("NotFoundException: {}");
        final Response.Status status = Response.Status.NOT_FOUND;
        String code = "not_found";
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        } else if (looksLikeMessageCode(message)) {
            code = message;
            message = localeMessages.msg(message);
        }
        return Response.status(status)
                .type(ErrorDto.mediaType())
                .entity(ErrorDto.of(status.getStatusCode(), code, message))
                .build();
    }

    private static boolean looksLikeMessageCode(final String message) {
        return message.indexOf(' ') < 0 && message.indexOf('.') > 0;
    }
}
