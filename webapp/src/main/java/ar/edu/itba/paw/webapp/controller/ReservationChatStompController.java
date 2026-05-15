package ar.edu.itba.paw.webapp.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import ar.edu.itba.paw.models.dto.ReservationMessageDto;
import ar.edu.itba.paw.services.ReservationMessageService;
import ar.edu.itba.paw.webapp.dto.PostReservationMessageRequest;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

@Controller
public final class ReservationChatStompController {

    private final ReservationMessageService reservationMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ReservationChatStompController(
            final ReservationMessageService reservationMessageService,
            final SimpMessagingTemplate messagingTemplate) {
        this.reservationMessageService = reservationMessageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/reservations/{reservationId}/messages")
    public void sendMessage(
            @DestinationVariable("reservationId") final long reservationId,
            @Payload final PostReservationMessageRequest request,
            final Principal principal) {
        final long senderUserId = resolveSenderUserId(principal);
        final String body = request != null ? request.getBody() : null;
        final ReservationMessageDto dto =
                reservationMessageService.postMessage(senderUserId, reservationId, body);
        messagingTemplate.convertAndSend("/topic/reservations/" + reservationId, dto);
    }

    private static long resolveSenderUserId(final Principal principal) {
        final Long fromPrincipal = userIdFromPrincipal(principal);
        if (fromPrincipal != null) {
            return fromPrincipal;
        }
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Long fromContext = userIdFromPrincipal(authentication);
        if (fromContext != null) {
            return fromContext;
        }
        throw new IllegalStateException("Unauthenticated WebSocket session");
    }

    private static Long userIdFromPrincipal(final Principal principal) {
        if (principal instanceof Authentication) {
            final Object authenticatedPrincipal = ((Authentication) principal).getPrincipal();
            if (authenticatedPrincipal instanceof RydenUserDetails) {
                return ((RydenUserDetails) authenticatedPrincipal).getUserId();
            }
        }
        return null;
    }
}
