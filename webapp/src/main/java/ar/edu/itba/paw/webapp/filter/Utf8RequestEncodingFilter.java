package ar.edu.itba.paw.webapp.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sets UTF-8 on the {@linkplain HttpServletRequest request} only.
 *
 * <p>Spring's {@code CharacterEncodingFilter} with {@code forceEncoding=true} also calls
 * {@code response.setCharacterEncoding}, which makes the container append
 * {@code ;charset=UTF-8} to binary {@code Content-Type} values (e.g.
 * {@code image/png;charset=UTF-8}). With global {@code X-Content-Type-Options: nosniff},
 * browsers then refuse to paint those bytes in {@code <img>} / media elements.</p>
 */
public final class Utf8RequestEncodingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        }
        filterChain.doFilter(request, response);
    }
}
