package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.webapp.dto.rest.ValidationErrorDto;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/** Maps invalid path/query/header/form parameters to {@code validation-error.v1+json}. */
@Provider
@Component
public final class ParamExceptionMapper implements ExceptionMapper<ParamException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParamExceptionMapper.class);

    private final LocaleMessages localeMessages;

    @Autowired
    public ParamExceptionMapper(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @Override
    public Response toResponse(final ParamException exception) {
        LOGGER.atDebug()
                .setCause(exception)
                .addArgument(exception.getParameterName())
                .log("ParamException parameter={}");
        final String message = localeMessages.msg(MessageKeys.ERROR_INVALID_REQUEST_PARAMETER);
        final ValidationErrorDto body = new ValidationErrorDto();
        body.setMessage(message);
        final ValidationErrorDto.FieldError fieldError = new ValidationErrorDto.FieldError();
        fieldError.setField(exception.getParameterName());
        fieldError.setMessage(message);
        body.setErrors(java.util.List.of(fieldError));
        return Response.status(Response.Status.BAD_REQUEST)
                .type(ValidationErrorDto.mediaType())
                .entity(body)
                .build();
    }
}
