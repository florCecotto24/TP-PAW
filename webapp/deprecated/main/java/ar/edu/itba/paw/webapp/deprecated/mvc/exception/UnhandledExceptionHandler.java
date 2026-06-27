package ar.edu.itba.paw.webapp.deprecated.mvc.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Fallback for unchecked controller failures that are not {@link ar.edu.itba.paw.exception.RydenException}.
 * Avoids exposing raw exception text to the user; details stay in server logs.
 * Checked exceptions and {@link javax.servlet.ServletException} subclasses are not handled here so Spring MVC
 * can apply its default handling where applicable.
 *
 * {@link ResponseStatus} aligns the HTTP status with the rendered view; {@code setStatus} is used
 * internally (not {@code sendError}) so the servlet container does not re-dispatch to
 * {@code <error-page>} after the view is rendered.
 */
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public final class UnhandledExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnhandledExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView onUnhandledRuntimeException(final RuntimeException ex) {
        LOGGER.atError().setCause(ex).log("Unhandled runtime exception in web tier");
        final ModelAndView mav = new ModelAndView("error");
        mav.addObject("statusCode", 500);
        mav.addObject("messageKey", "error.500");
        return mav;
    }
}
