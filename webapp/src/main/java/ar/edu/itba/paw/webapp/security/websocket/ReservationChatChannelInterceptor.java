package ar.edu.itba.paw.webapp.security.websocket;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationMessageService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

@Component
public final class ReservationChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_RESERVATION =
            Pattern.compile("^/topic/reservations/(\\d+)$");
    private static final Pattern APP_SEND_MESSAGE =
            Pattern.compile("^/app/reservations/(\\d+)/messages$");

    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationChatChannelInterceptor(final ReservationMessageService reservationMessageService) {
        this.reservationMessageService = reservationMessageService;
    }

    @Override
    public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        final StompCommand command = accessor.getCommand();
        if (command != StompCommand.SUBSCRIBE && command != StompCommand.SEND) {
            return message;
        }
        final String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }
        final Long reservationId = parseReservationId(destination);
        if (reservationId == null) {
            return message;
        }
        final long viewerUserId = resolveUserId(accessor.getUser());
        if (!reservationMessageService.canParticipantAccessReservationChat(viewerUserId, reservationId)) {
            throw new AccessDeniedException("Reservation chat not allowed for user " + viewerUserId);
        }
        return message;
    }

    @Nullable
    private static Long parseReservationId(final String destination) {
        Matcher matcher = TOPIC_RESERVATION.matcher(destination);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(1));
        }
        matcher = APP_SEND_MESSAGE.matcher(destination);
        if (matcher.matches()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private static long resolveUserId(@Nullable final Principal principal) {
        if (principal instanceof Authentication) {
            final Object authenticatedPrincipal = ((Authentication) principal).getPrincipal();
            if (authenticatedPrincipal instanceof RydenUserDetails) {
                return ((RydenUserDetails) authenticatedPrincipal).getUserId();
            }
        }
        throw new AccessDeniedException("Unauthenticated WebSocket session");
    }
}
