package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ar.edu.itba.paw.webapp.dto.rest.ErrorDto;

@Provider
public final class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessDeniedExceptionMapper.class);

    @Override
    public Response toResponse(final AccessDeniedException exception) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final boolean anonymous = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
        final Response.Status status = anonymous ? Response.Status.UNAUTHORIZED : Response.Status.FORBIDDEN;
        LOGGER.debug("AccessDeniedException status={}", status.getStatusCode());
        final Response.ResponseBuilder builder = Response.status(status)
                .type(ErrorDto.mediaType())
                .entity(new ErrorDto(
                        status.getStatusCode(),
                        anonymous ? "unauthorized" : "forbidden",
                        exception.getMessage()));
        if (anonymous) {
            builder.header("WWW-Authenticate", "Basic realm=\"ryden\", Bearer realm=\"ryden\"");
        }
        return builder.build();
    }
}
