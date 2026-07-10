package ar.edu.itba.paw.webapp.exception.mapper;

import java.util.Locale;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
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
 * Catch-all for {@link WebApplicationException} subtypes thrown directly by controllers/support
 * classes ({@code ForbiddenException}, {@code NotFoundException}, {@code BadRequestException},
 * {@code NotAuthorizedException}, {@code NotAllowedException}, ...) that don't have a more
 * specific mapper. Without this, Jersey lets the container render its default HTML error page
 * for them instead of {@code application/vnd.paw.error.v1+json}.
 *
 * <p>Messages are author-controlled strings passed at the throw site in this codebase (never a
 * raw {@code Throwable#getMessage()} from an unexpected underlying cause), so it's safe to surface
 * them. When the message looks like a {@code MessageKeys} dotted code (no spaces), it's resolved
 * through {@link LocaleMessages} the same way {@link RydenExceptionMapper} does; otherwise it's
 * used as-is, and if absent the HTTP reason phrase is used.</p>
 */
@Provider
@Component
public final class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);

    private final LocaleMessages localeMessages;

    @Autowired
    public WebApplicationExceptionMapper(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @Override
    public Response toResponse(final WebApplicationException exception) {
        final Response original = exception.getResponse();
        Response.Status status = Response.Status.fromStatusCode(original.getStatus());
        if (status == null) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }
        // Jersey often wraps multipart/parse failures as a bare BadRequestException
        // ("HTTP 400 Bad Request"); log the cause so local debugging can see why.
        if (exception.getCause() != null) {
            LOGGER.atDebug()
                    .setMessage("{} status={} message={} cause={}")
                    .addArgument(exception.getClass().getSimpleName())
                    .addArgument(status.getStatusCode())
                    .addArgument(exception.getMessage())
                    .addArgument(exception.getCause().toString())
                    .log();
        } else {
            LOGGER.atDebug()
                    .setMessage("{} status={} message={}")
                    .addArgument(exception.getClass().getSimpleName())
                    .addArgument(status.getStatusCode())
                    .addArgument(exception.getMessage())
                    .log();
        }

        String code = statusSlug(status);
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        } else if (looksLikeMessageCode(message)) {
            code = message;
            message = localeMessages.msg(message);
        }

        final Response.ResponseBuilder builder = Response.status(status)
                .type(ErrorDto.mediaType())
                .entity(ErrorDto.of(status.getStatusCode(), code, message));
        copyChallengeHeaders(original.getHeaders(), builder);
        return builder.build();
    }

    /** Preserves headers the throw site set on the original response (e.g. {@code Allow}, {@code WWW-Authenticate}). */
    private static void copyChallengeHeaders(
            final MultivaluedMap<String, Object> originalHeaders,
            final Response.ResponseBuilder builder) {
        originalHeaders.forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
    }

    private static boolean looksLikeMessageCode(final String message) {
        return message.indexOf(' ') < 0 && message.indexOf('.') > 0;
    }

    private static String statusSlug(final Response.Status status) {
        return status.name().toLowerCase(Locale.ROOT);
    }
}
