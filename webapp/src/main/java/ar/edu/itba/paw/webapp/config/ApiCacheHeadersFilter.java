package ar.edu.itba.paw.webapp.config;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Default cache policy for vendor-JSON GET responses under the REST API.
 *
 * <p>Anonymous reads use {@code Cache-Control: no-store} so intermediaries do not keep public
 * representations that may change when the caller later authenticates. Authenticated reads use
 * {@code no-cache} (revalidate on each use). {@code Vary: Accept} is always set for negotiated
 * JSON because the same URI can yield summary vs full representations.</p>
 *
 * <p>Skips responses that already carry {@code Cache-Control} or {@code ETag} — notably binary
 * media served via {@link ar.edu.itba.paw.webapp.support.CacheableBinaryResponses} (A4).</p>
 */
@Provider
public final class ApiCacheHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(
            final ContainerRequestContext requestContext, final ContainerResponseContext responseContext) {
        if (!HttpMethod.GET.matches(requestContext.getMethod())) {
            return;
        }
        if (responseContext.getHeaders().containsKey(HttpHeaders.CACHE_CONTROL)
                || responseContext.getHeaders().containsKey(HttpHeaders.ETAG)) {
            return;
        }
        final MediaType mediaType = responseContext.getMediaType();
        if (!isJsonFamily(mediaType)) {
            return;
        }
        appendVaryAccept(responseContext);
        responseContext.getHeaders().putSingle(
                HttpHeaders.CACHE_CONTROL, isAuthenticated() ? "no-cache" : "no-store");
    }

    private static boolean isJsonFamily(final MediaType mediaType) {
        if (mediaType == null || !"application".equalsIgnoreCase(mediaType.getType())) {
            return false;
        }
        final String subtype = mediaType.getSubtype();
        return "json".equalsIgnoreCase(subtype) || subtype.endsWith("+json");
    }

    private static boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static void appendVaryAccept(final ContainerResponseContext responseContext) {
        final Object existing = responseContext.getHeaders().getFirst(HttpHeaders.VARY);
        if (existing == null) {
            responseContext.getHeaders().putSingle(HttpHeaders.VARY, "Accept");
            return;
        }
        final String existingValue = existing.toString();
        if (existingValue.toLowerCase().contains("accept")) {
            return;
        }
        responseContext.getHeaders().putSingle(HttpHeaders.VARY, existingValue + ", Accept");
    }
}
