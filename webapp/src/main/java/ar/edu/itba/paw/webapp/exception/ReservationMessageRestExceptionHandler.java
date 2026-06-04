package ar.edu.itba.paw.webapp.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.edu.itba.paw.exception.reservation.ReservationMessageException;
import ar.edu.itba.paw.webapp.controller.ReservationChatPollController;
import ar.edu.itba.paw.webapp.controller.ReservationMessageController;
import ar.edu.itba.paw.webapp.util.LocaleMessages;

/** JSON errors for reservation chat REST endpoints. */
@RestControllerAdvice(assignableTypes = {ReservationMessageController.class, ReservationChatPollController.class})
public final class ReservationMessageRestExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationMessageRestExceptionHandler.class);

    private final LocaleMessages localeMessages;

    public ReservationMessageRestExceptionHandler(final LocaleMessages localeMessages) {
        this.localeMessages = localeMessages;
    }

    @ExceptionHandler(ReservationMessageException.class)
    public ResponseEntity<Map<String, String>> onReservationMessageException(
            final ReservationMessageException exception) {
        LOGGER.debug("Reservation chat REST rejected: {}", exception.getMessageCode());
        final String message = localeMessages.msg(exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", exception.getMessageCode(), "message", message));
    }
}
