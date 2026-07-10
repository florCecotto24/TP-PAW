package ar.edu.itba.paw.webapp.exception.mapper;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.itba.paw.webapp.dto.rest.ValidationErrorDto;

@Provider
public final class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    @Override
    public Response toResponse(final ConstraintViolationException exception) {
        LOGGER.atDebug()
                .addArgument(exception.getConstraintViolations())
                .log("ConstraintViolationException: {}");
        final ValidationErrorDto body = ValidationErrorDto.fromConstraintViolations(exception.getConstraintViolations());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(ValidationErrorDto.mediaType())
                .entity(body)
                .build();
    }
}
