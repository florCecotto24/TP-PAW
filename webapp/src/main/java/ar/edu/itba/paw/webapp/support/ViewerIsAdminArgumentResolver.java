package ar.edu.itba.paw.webapp.support;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;

/**
 * Resolves {@code boolean} controller parameters annotated with {@link ViewerIsAdmin} by
 * inspecting the session principal in {@link SecurityContextHolder}. Centralises the
 * authorities check so controllers don't have to inject {@code Authentication} or
 * inline the role lookup.
 */
public final class ViewerIsAdminArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ViewerIsAdmin.class)
                && (parameter.getParameterType().equals(boolean.class)
                        || parameter.getParameterType().equals(Boolean.class));
    }

    @Override
    public Object resolveArgument(
            final MethodParameter parameter,
            final ModelAndViewContainer mavContainer,
            final NativeWebRequest webRequest,
            final WebDataBinderFactory binderFactory) {
        return AuthenticationAuthorities.hasAdminRole(
                SecurityContextHolder.getContext().getAuthentication());
    }
}
