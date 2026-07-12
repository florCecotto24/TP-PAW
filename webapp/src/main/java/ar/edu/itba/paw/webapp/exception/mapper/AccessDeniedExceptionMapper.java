package ar.edu.itba.paw.webapp.exception.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.webapp.dto.rest.ErrorDto;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@Provider
@Component
public final class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessDeniedExceptionMapper.class);

    private final LocaleMessages localeMessages;

    @Autowired
    public AccessDeniedExceptionMapper(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @Override
    public Response toResponse(final AccessDeniedException exception) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final boolean anonymous = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
        final Response.Status status = anonymous ? Response.Status.UNAUTHORIZED : Response.Status.FORBIDDEN;
        LOGGER.atDebug()
                .setCause(exception)
                .addArgument(status.getStatusCode())
                .log("AccessDeniedException status={}");
        final String code = anonymous ? MessageKeys.ERROR_UNAUTHORIZED : MessageKeys.ERROR_FORBIDDEN;
        final Response.ResponseBuilder builder = Response.status(status)
                .type(ErrorDto.mediaType())
                .entity(ErrorDto.of(
                        status.getStatusCode(),
                        code,
                        localeMessages.msg(code)));
        if (anonymous) {
            // Bearer only — see WebAuthConfig#restAuthenticationEntryPoint for why "Basic" must not be
            // advertised here (native browser credential popup on any 401 that reaches a real browser).
            builder.header("WWW-Authenticate", "Bearer realm=\"ryden\"");
        }
        return builder.build();
    }
}
