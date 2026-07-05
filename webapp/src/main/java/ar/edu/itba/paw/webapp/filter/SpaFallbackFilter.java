package ar.edu.itba.paw.webapp.filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Serves {@code index.html} for SPA deep-links before Jersey/Security reject unknown HTML paths,
 * and routes static assets straight to the container's {@code default} servlet before Jersey.
 * Vendor JSON ({@code Accept: application/vnd.paw.*}) and binary sub-resources ({@code image/*},
 * {@code video/*}, etc.) pass through to Jersey unchanged.
 */
public final class SpaFallbackFilter extends OncePerRequestFilter {

    private static final String INDEX_HTML = "/index.html";
    private static final String VND_PAW_ACCEPT_PREFIX = "application/vnd.paw.";

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        if (isStaticAsset(request)) {
            // Bypass Jersey entirely: the jersey filter is mapped to "/*" (its @Path resources
            // assume the request path is matched from the context root), so it can't be scoped to
            // exclude these prefixes without shifting how it resolves every other @Path. Since none
            // of the filters declared below (jersey included) request FORWARD dispatch in web.xml,
            // this forward skips straight to the "default" servlet mapping for /public|/assets|etc.
            // Without this, an unmatched path here becomes Jersey's own internal NotFoundException,
            // which WebApplicationExceptionMapper (a catch-all with no way to tell "no route matched"
            // apart from an application-thrown 404) turns into a real JSON 404 response — defeating
            // jersey.config.servlet.filter.forwardOn404 and breaking every static asset (see B4).
            request.getRequestDispatcher(staticAssetPath(request)).forward(request, response);
            return;
        }

        if (!"GET".equalsIgnoreCase(request.getMethod())
                || isApiRequest(request)
                || !acceptsHtmlDocument(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (indexExists(request.getServletContext())) {
            request.getRequestDispatcher(INDEX_HTML).forward(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isApiRequest(final HttpServletRequest request) {
        final String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(VND_PAW_ACCEPT_PREFIX);
    }

    /**
     * {@code <img>} / {@code <video>} and other byte endpoints use {@code image/*} (not {@code text/html}).
     * Only browser navigations should receive the SPA shell.
     */
    private static boolean acceptsHtmlDocument(final HttpServletRequest request) {
        final String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept == null || accept.isBlank()) {
            return false;
        }
        return accept.contains("text/html") || accept.contains("application/xhtml+xml");
    }

    private static boolean isStaticAsset(final HttpServletRequest request) {
        final String path = staticAssetPath(request);
        return path.startsWith("/public/") || path.startsWith("/assets/") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/WEB-INF/");
    }

    private static String staticAssetPath(final HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static boolean indexExists(final ServletContext servletContext) {
        final String realPath = servletContext.getRealPath(INDEX_HTML);
        if (realPath == null) {
            return servletContext.getResourceAsStream(INDEX_HTML) != null;
        }
        return Files.exists(Paths.get(realPath));
    }
}
