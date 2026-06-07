package ar.edu.itba.paw.webapp.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.webapp.support.SafeRefererResolver;

/** Maps {@code /error} to friendly views or same-app Referer redirects when appropriate. */
@Controller
public final class ErrorController {

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
            final String sameAppPath = SafeRefererResolver.sameAppRelativePath(
                    request, request.getHeader("Referer"));
            if (sameAppPath != null && !sameAppPath.startsWith("/error")) {
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
}
