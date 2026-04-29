package ar.edu.itba.paw.webapp.advice;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

@ControllerAdvice
@Order(0)
public final class RydenExceptionHandler {

    private final LocaleMessages localeMessages;

    public RydenExceptionHandler(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @ExceptionHandler(RydenException.class)
    public ModelAndView onRydenException(final RydenException ex) {
        final ModelAndView mav = new ModelAndView("error");
        mav.addObject("statusCode", 400);
        mav.addObject("messageKey", "error.ryden");
        mav.addObject("exceptionMessage", localeMessages.msg(ex));
        return mav;
    }
}
