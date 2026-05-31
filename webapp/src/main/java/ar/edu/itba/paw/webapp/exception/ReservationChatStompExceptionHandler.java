package ar.edu.itba.paw.webapp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

import ar.edu.itba.paw.exception.reservation.ReservationMessageException;

/**
 * Global handler for failures raised by STOMP {@code @MessageMapping} methods (notably the
 * reservation chat). Maps them to STOMP ERROR frames via Spring's messaging infrastructure
 * instead of letting the unhandled exception close the WebSocket session abruptly.
 *
 * <p><b>Why {@link ControllerAdvice} (and not {@link org.springframework.stereotype.Controller}):</b>
 * {@link MessageExceptionHandler} methods only apply to {@code @MessageMapping} handlers declared in
 * the <em>same</em> class unless they live inside a {@code @ControllerAdvice} bean — that is the only
 * way Spring's {@code SimpAnnotationMethodMessageHandler} treats them as a global / cross-controller
 * fallback for the entire messaging stack.</p>
 */
@ControllerAdvice
public final class ReservationChatStompExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationChatStompExceptionHandler.class);

    @MessageExceptionHandler(ReservationMessageException.class)
    public void handleReservationMessageException(final ReservationMessageException exception) {
        LOGGER.debug("Reservation chat message rejected: {}", exception.getMessageCode());
    }

    @MessageExceptionHandler(IllegalStateException.class)
    public void handleIllegalState(final IllegalStateException exception) {
        LOGGER.warn("Reservation chat STOMP handler failed", exception);
    }
}
