package ar.edu.itba.paw.webapp.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import ar.edu.itba.paw.webapp.i18n.RydenLocaleResolver;

/**
 * Populates {@link LocaleContextHolder} from {@link RydenLocaleResolver} for the request.
 */
public final class RydenLocaleFilter extends OncePerRequestFilter {

    private final RydenLocaleResolver localeResolver;

    public RydenLocaleFilter(final RydenLocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
        try {
            filterChain.doFilter(request, response);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
