package ar.edu.itba.paw.webapp.support;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

/**
 * Same-application Referer validation. Centralizes the host/scheme/port comparison previously
 * scattered between {@code ErrorController} and the navbar redirect endpoints, so that any
 * controller that wants to bounce the user back to "where they came from" cannot be turned
 * into an open redirect by an attacker-controlled {@code Referer} header.
 *
 * Returns the same-origin path+query (relative to context) when the {@code Referer} parses
 * cleanly and points to this very application; returns {@code null} otherwise so the caller
 * can fall back to a safe default route.
 */
public final class SafeRefererResolver {

    private SafeRefererResolver() {
    }

    /**
     * Returns the relative same-app path (with optional {@code ?query}) extracted from the
     * supplied {@code Referer}, or {@code null} when the referer is missing, malformed, points
     * to a different host/scheme/port, escapes the context, or contains traversal segments.
     */
    public static String sameAppRelativePath(final HttpServletRequest request, final String refererHeader) {
        if (refererHeader == null || refererHeader.isBlank()) {
            return null;
        }
        final String referer = refererHeader.trim();
        if (!referer.regionMatches(true, 0, "http://", 0, 7)
                && !referer.regionMatches(true, 0, "https://", 0, 8)) {
            return null;
        }
        try {
            final URI ref = URI.create(referer);
            if (ref.getHost() == null) {
                return null;
            }
            final URI here = URI.create(request.getRequestURL().toString());
            if (!ref.getScheme().equalsIgnoreCase(here.getScheme())
                    || !ref.getHost().equalsIgnoreCase(here.getHost())
                    || ref.getPort() != here.getPort()) {
                return null;
            }
            String path = ref.getRawPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            final String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                if (!path.startsWith(contextPath)) {
                    return null;
                }
                path = path.substring(contextPath.length());
            }
            if (path.isEmpty()) {
                path = "/";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            // Reject traversal and double-slash that could change resolution under reverse proxies.
            if (path.contains("//") || path.contains("..")) {
                return null;
            }
            final String query = ref.getRawQuery();
            return (query != null && !query.isBlank()) ? path + "?" + query : path;
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Same as {@link #sameAppRelativePath(HttpServletRequest, String)} but returns {@code defaultPath}
     * when the referer is unsafe or absent. Use when the caller always wants a non-null target.
     */
    public static String sameAppRelativePathOrDefault(
            final HttpServletRequest request, final String refererHeader, final String defaultPath) {
        final String resolved = sameAppRelativePath(request, refererHeader);
        return resolved != null ? resolved : defaultPath;
    }
}
