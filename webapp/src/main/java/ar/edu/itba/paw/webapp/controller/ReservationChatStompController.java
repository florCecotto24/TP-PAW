package ar.edu.itba.paw.webapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
            @AuthenticationPrincipal final RydenUserDetails sender) {
        if (sender == null) {
            throw new IllegalStateException("Unauthenticated WebSocket session");
        }
        final long senderUserId = sender.getUserId();
        final String body = request != null ? request.getBody() : null;
        final ReservationMessageDto dto =
                reservationMessageService.postMessage(senderUserId, reservationId, body);
        messagingTemplate.convertAndSend("/topic/reservations/" + reservationId, dto);
    }
}
