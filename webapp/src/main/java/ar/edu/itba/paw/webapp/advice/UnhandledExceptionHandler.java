package ar.edu.itba.paw.webapp.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Fallback for unchecked controller failures that are not {@link ar.edu.itba.paw.exception.RydenException}.
 * Avoids exposing raw exception text to the user; details stay in server logs.
 * Checked exceptions and {@link javax.servlet.ServletException} subclasses are not handled here so Spring MVC
 * can apply its default handling where applicable.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public final class UnhandledExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnhandledExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView onUnhandledRuntimeException(final RuntimeException ex) {
        LOGGER.atError().setCause(ex).log("Unhandled runtime exception in web tier");
        final ModelAndView mav = new ModelAndView("error");
        mav.addObject("statusCode", 500);
        mav.addObject("messageKey", "error.500");
        return mav;
    }
}
