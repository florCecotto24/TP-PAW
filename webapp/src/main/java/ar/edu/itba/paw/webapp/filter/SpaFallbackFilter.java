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
 * Serves {@code index.html} for SPA deep-links before Jersey/Security reject unknown HTML paths.
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
        if (!"GET".equalsIgnoreCase(request.getMethod())
                || isApiRequest(request)
                || isStaticAsset(request)
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
        final String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/public/") || path.startsWith("/assets/") || path.startsWith("/css/")
                || path.startsWith("/js/") || path.startsWith("/WEB-INF/");
    }

    private static boolean indexExists(final ServletContext servletContext) {
        final String realPath = servletContext.getRealPath(INDEX_HTML);
        if (realPath == null) {
            return servletContext.getResourceAsStream(INDEX_HTML) != null;
        }
        return Files.exists(Paths.get(realPath));
    }
}
