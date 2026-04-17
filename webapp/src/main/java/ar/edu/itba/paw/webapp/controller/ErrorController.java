package ar.edu.itba.paw.webapp.controller;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorController {

    @RequestMapping("/error")
    public ModelAndView error(final HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            statusCode = 500;
        }

        /*
         * Client frequently errors: if the browser sends a Referer of the same application, we redirect to that screen instead of the home or the generic /error view.
         * Server errors (e.g. 500) are not handled here and continue to display the "error" view.
         */
        if (statusCode == 404 || statusCode == 403 || statusCode == 405) {
            final String sameAppPath = safeSameApplicationRefererPath(request);
            if (sameAppPath != null) {
                return new ModelAndView("redirect:" + sameAppPath);
            }
        }

        final ModelAndView mav = new ModelAndView("error");

        final String messageKey;
        switch (statusCode) {
            case 400: messageKey = "error.400"; break;
            case 403: messageKey = "error.403"; break;
            case 404: messageKey = "error.404"; break;
            case 405: messageKey = "error.405"; break;
            default:  messageKey = "error.500"; break;
        }

        mav.addObject("statusCode", statusCode);
        mav.addObject("messageKey", messageKey);
        return mav;
    }

    /**
     * Returns the relative path to the context (e.g. {@code /search}) ready to be used in {@code redirect:...}, or {@code null}.
     */
    private static String safeSameApplicationRefererPath(final HttpServletRequest request) {
        final String refererHeader = request.getHeader("Referer");
        if (refererHeader == null || refererHeader.isBlank()) {
            return null;
        }
        final String referer = refererHeader.trim();
        if (!referer.regionMatches(true, 0, "http://", 0, 7) && !referer.regionMatches(true, 0, "https://", 0, 8)) {
            return null;
        }
        try {
            final URI ref = URI.create(referer);
            if (ref.getHost() == null) {
                return null;
            }
            final StringBuffer thisUrl = request.getRequestURL();
            final URI here = URI.create(thisUrl.toString());
            if (!ref.getScheme().equalsIgnoreCase(here.getScheme())
                    || !ref.getHost().equalsIgnoreCase(here.getHost())) {
                return null;
            }
            if (ref.getPort() != here.getPort()) {
                return null;
            }
            String path = ref.getRawPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            final String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty()) {
                if (!path.startsWith(contextPath)) {
                    return null;
                }
                path = path.substring(contextPath.length());
            }
            if (path.isEmpty()) {
                path = "/";
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.startsWith("/error") || path.contains("//") || path.contains("..")) {
                return null;
            }
            final String query = ref.getRawQuery();
            if (query != null && !query.isBlank()) {
                return path + "?" + query;
            }
            return path;
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }
}
