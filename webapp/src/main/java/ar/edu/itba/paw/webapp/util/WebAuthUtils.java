package ar.edu.itba.paw.webapp.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/** Helpers to read {@link RydenUserDetails} / {@link User} from Spring Security and safe redirect targets. */
public final class WebAuthUtils {

    private static final Logger LOG = LoggerFactory.getLogger(WebAuthUtils.class);

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
            LOG.atDebug()
                    .setMessage("Referer not a valid URI for guest-only redirect [{}]")
                    .addArgument(referer)
                    .setCause(e)
                    .log();
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
        if (contextPath.isEmpty()) {
            if (inApp.startsWith("/webapp/")) {
                inApp = inApp.substring("/webapp".length());
            } else if ("/webapp".equals(inApp)) {
                inApp = "/";
            }
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

    public static Optional<User> viewerUser(final Authentication authentication) {
        // The roleAssignedBy slot stays populated so admin-only handlers that need to know who
        // granted the role can read it through @CurrentUser instead of falling back to
        // Authentication / RydenUserDetails directly.
        return currentUserDetails(authentication).map(
                d -> User.builder()
                        .id(d.getUserId())
                        .email(d.getUsername())
                        .forename(d.getForename())
                        .surname(d.getSurname())
                        .roleAssignedBy(d.getRoleAssignedBy().orElse(null))
                        .build());
    }

    public static Optional<RydenUserDetails> currentUserDetails(final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof RydenUserDetails details)) {
            return Optional.empty();
        }
        return Optional.of(details);
    }

    public static RydenUserDetails requireCurrentUser(final Authentication authentication) {
        return currentUserDetails(authentication).orElseThrow(() -> new IllegalStateException("Expected authenticated user"));
    }

    /**
     * Domain user injected with {@link ar.edu.itba.paw.webapp.support.CurrentUser} (and exposed in the model by {@link ar.edu.itba.paw.webapp.advice.LoggedUserAdvice} when signed in); same contract as {@link #requireCurrentUser(Authentication)}.
     */
    public static User requireUser(final User currentUser) {
        if (currentUser == null) {
            throw new IllegalStateException("Expected authenticated user");
        }
        return currentUser;
    }

    public static boolean isSignedIn(final User currentUser) {
        return currentUser != null;
    }
}
