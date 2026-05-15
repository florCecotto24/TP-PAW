package ar.edu.itba.paw.webapp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Controller;

import ar.edu.itba.paw.exception.reservation.ReservationMessageException;

/**
 * Maps reservation chat failures to STOMP ERROR frames instead of closing the socket with an
 * unhandled server exception.
 */
@Controller
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
