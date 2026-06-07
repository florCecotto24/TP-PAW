package ar.edu.itba.paw.webapp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.exception.RydenException;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/**
 * Maps {@link RydenException} to the generic error view with HTTP 400 and a localized message from the exception key.
 */
@ControllerAdvice
@Order(0)
public final class RydenExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RydenExceptionHandler.class);

    private final LocaleMessages localeMessages;

    public RydenExceptionHandler(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @ExceptionHandler(RydenException.class)
    public ModelAndView onRydenException(final RydenException ex) {
        LOGGER.atDebug()
                .setCause(ex)
                .addArgument(ex.getMessageCode())
                .log("RydenException messageKey={}");
        final ModelAndView mav = new ModelAndView("error");
        mav.addObject("statusCode", 400);
        mav.addObject("messageKey", "error.ryden");
        mav.addObject("exceptionMessage", localeMessages.msg(ex));
        return mav;
    }

    /**
     * Method-level validation failures (e.g. {@code @Min(0) int page} on @RequestParam) bubble up as
     * ConstraintViolationException. We map them to the same generic 400 error view as RydenException
     * so users that hand-craft URLs with negative pages see the standard error page instead of a 500.
     */
    @ExceptionHandler(javax.validation.ConstraintViolationException.class)
    public ModelAndView onConstraintViolation(final javax.validation.ConstraintViolationException ex) {
        LOGGER.atDebug().setCause(ex).log("ConstraintViolationException on request param");
        final ModelAndView mav = new ModelAndView("error");
        mav.addObject("statusCode", 400);
        mav.addObject("messageKey", "error.ryden");
        mav.addObject("exceptionMessage", ex.getConstraintViolations().stream()
                .findFirst()
                .map(javax.validation.ConstraintViolation::getMessage)
                .orElse("Invalid request parameter"));
        return mav;
    }
}
