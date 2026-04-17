package ar.edu.itba.paw.webapp.util;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import ar.edu.itba.paw.webapp.security.RydenUserDetails;

public final class WebAuthUtils {

    private WebAuthUtils() {
    }

    /**
     * Path within the application (e.g. {@code /search}) to use in {@code redirect:...} when a authenticated user visits a URL intended only for guests.
     * If the {@code Referer} points to this same app, it is reused; otherwise, {@code /}. Avoid loops if the referer is the own excluded page (e.g. {@code /login}).
     */
    public static String guestOnlyPageRedirectTarget(final HttpServletRequest request, final String excludedInAppPath) {
        final String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        final URI uri;
        try {
            uri = new URI(referer);
        } catch (final URISyntaxException e) {
            return "/";
        }
        final String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return "/";
        }
        if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(request.getServerName())) {
            return "/";
        }
        if (effectivePort(uri) != request.getServerPort()) {
            return "/";
        }
        final String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
        final String refPath = uri.getRawPath() != null ? uri.getRawPath() : "";
        if (!refPath.startsWith(contextPath.isEmpty() ? "/" : contextPath)) {
            return "/";
        }
        String inApp = refPath.substring(contextPath.length());
        if (inApp.isEmpty()) {
            inApp = "/";
        } else if (!inApp.startsWith("/")) {
            inApp = "/" + inApp;
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            inApp = inApp + "?" + uri.getRawQuery();
        }
        if (inApp.contains("..") || inApp.startsWith("//")) {
            return "/";
        }
        final String pathOnly = inApp.contains("?") ? inApp.substring(0, inApp.indexOf('?')) : inApp;
        if (pathOnly.equals(excludedInAppPath) || pathOnly.startsWith(excludedInAppPath + "/")) {
            return "/";
        }
        return inApp;
    }

    private static int effectivePort(final URI uri) {
        final int p = uri.getPort();
        if (p != -1) {
            return p;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        }
        return 80;
    }

    public static RydenUserDetails requireCurrentUser(final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof RydenUserDetails)) {
            throw new IllegalStateException("Expected authenticated user");
        }
        return (RydenUserDetails) authentication.getPrincipal();
    }
}
