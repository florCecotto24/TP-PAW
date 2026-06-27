package ar.edu.itba.paw.webapp.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cache-Control for revved SPA assets ({@code /public}) and semi-static assets ({@code /assets}).
 * LINEAMIENTOS §4.2–§4.3.
 */
public final class UnconditionalCacheFilter extends OncePerRequestFilter {

    private static final int REVVED_MAX_AGE = 31536000;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.GET.matches(request.getMethod())) {
            final String path = request.getRequestURI().substring(request.getContextPath().length());
            if (path.startsWith("/public/")) {
                response.setHeader(HttpHeaders.CACHE_CONTROL,
                        String.format("public, max-age=%d, immutable", REVVED_MAX_AGE));
            } else if (path.startsWith("/assets/")) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=0, must-revalidate");
            } else if ("/index.html".equals(path) || "/".equals(path)) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
            }
        }
        filterChain.doFilter(request, response);
    }
}
