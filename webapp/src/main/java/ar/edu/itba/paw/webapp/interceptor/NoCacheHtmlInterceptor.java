package ar.edu.itba.paw.webapp.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Prevents browsers from caching HTML responses. Home and other pages vary by session (e.g. listing cards),
 * so reusing a cached {@code GET /} after login can show stale markup and broken asset URLs.
 */
@Component
public final class NoCacheHtmlInterceptor implements HandlerInterceptor {

    private static final String CACHE_CONTROL = "no-cache, no-store, must-revalidate";

    @Override
    public void postHandle(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final Object handler,
            final ModelAndView modelAndView) {
        if (!shouldApplyNoCache(request, response, handler, modelAndView)) {
            return;
        }
        response.setHeader("Cache-Control", CACHE_CONTROL);
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0L);
        response.addHeader("Vary", "Cookie");
    }

    private static boolean shouldApplyNoCache(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler,
            final ModelAndView modelAndView) {
        if (response.isCommitted()) {
            return false;
        }
        if (handler instanceof HandlerMethod handlerMethod) {
            if (handlerMethod.getMethodAnnotation(org.springframework.web.bind.annotation.ResponseBody.class) != null) {
                return false;
            }
            if (handlerMethod.getBeanType().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class)) {
                return false;
            }
        }
        if (modelAndView != null && modelAndView.getViewName() != null && isRedirectView(modelAndView.getViewName())) {
            return false;
        }
        final String contentType = response.getContentType();
        if (contentType != null && !contentType.regionMatches(true, 0, MediaType.TEXT_HTML_VALUE, 0, MediaType.TEXT_HTML_VALUE.length())) {
            return false;
        }
        final String path = request.getRequestURI();
        return path == null || !isStaticAssetPath(path, request.getContextPath());
    }

    private static boolean isRedirectView(final String viewName) {
        return viewName.startsWith("redirect:");
    }

    private static boolean isStaticAssetPath(final String requestUri, final String contextPath) {
        String path = requestUri;
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/assets/")
                || path.startsWith("/image/");
    }
}
