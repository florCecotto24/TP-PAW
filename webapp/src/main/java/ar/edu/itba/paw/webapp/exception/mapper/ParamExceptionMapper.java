package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ParamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.paw.webapp.dto.rest.ValidationErrorDto;

/** Maps invalid path/query/header/form parameters to {@code validation-error.v1+json}. */
@Provider
public final class ParamExceptionMapper implements ExceptionMapper<ParamException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParamExceptionMapper.class);

    @Override
    public Response toResponse(final ParamException exception) {
        LOGGER.atDebug()
                .addArgument(exception.getParameterName())
                .log("ParamException parameter={}");
        final ValidationErrorDto body = new ValidationErrorDto();
        body.setMessage("Invalid request parameter.");
        final ValidationErrorDto.FieldError fieldError = new ValidationErrorDto.FieldError();
        fieldError.setField(exception.getParameterName());
        fieldError.setMessage(exception.getMessage());
        body.setErrors(java.util.List.of(fieldError));
        return Response.status(Response.Status.BAD_REQUEST)
                .type(ValidationErrorDto.mediaType())
                .entity(body)
                .build();
    }
}
