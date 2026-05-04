package ar.edu.itba.paw.webapp.security.http;

import java.util.OptionalLong;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Parses numeric ids from {@link HttpServletRequest} paths (after context path).
 */
public final class HttpRequestPathIds {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestPathIds.class);

    private HttpRequestPathIds() {
    }

    /**
     * Returns the first path segment after {@code prefix} if it is a positive long.
     * Prefix must start with {@code /} and usually end with {@code /} (e.g. {@code /my-listings/}).
     */
    public static OptionalLong firstLongSegmentAfterPrefix(final HttpServletRequest request, final String prefix) {
        final String path = normalizedPath(request);
        if (!path.startsWith(prefix)) {
            return OptionalLong.empty();
        }
        final String rest = path.substring(prefix.length());
        if (rest.isEmpty()) {
            return OptionalLong.empty();
        }
        final int slash = rest.indexOf('/');
        final String idPart = slash < 0 ? rest : rest.substring(0, slash);
        if (idPart.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            final long id = Long.parseLong(idPart);
            return id > 0 ? OptionalLong.of(id) : OptionalLong.empty();
        } catch (final NumberFormatException e) {
            LOG.atDebug()
                    .setMessage("Path id segment not a positive long after prefix [{}] idPart=[{}]")
                    .addArgument(prefix)
                    .addArgument(idPart)
                    .setCause(e)
                    .log();
            return OptionalLong.empty();
        }
    }

    private static String normalizedPath(final HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
            final String context = request.getContextPath();
            if (context != null && !context.isEmpty() && path.startsWith(context)) {
                path = path.substring(context.length());
            }
        }
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
