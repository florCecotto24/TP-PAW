package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ErrorController {

    @RequestMapping("/error")
    public ModelAndView error(HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("error");

        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            statusCode = 500;
        }

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
