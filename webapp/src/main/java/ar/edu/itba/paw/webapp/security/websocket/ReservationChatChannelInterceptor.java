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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.services.ReservationMessageService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

/**
 * STOMP {@link ChannelInterceptor} that mirrors the HTTP-side {@code ReservationWebAuthorization} rules
 * for the reservation chat channel:
 * <ul>
 *   <li><b>SEND</b> to {@code /app/reservations/{id}/messages}: rider/owner of the reservation only.
 *       Admins are excluded so they cannot impersonate a participant when posting messages.</li>
 *   <li><b>SUBSCRIBE</b> to {@code /topic/reservations/{id}}: rider/owner <em>or</em> any caller
 *       carrying the {@link UserRole#ADMIN} authority. This gives admins the read-only audit view
 *       expected by the {@code /admin/reservations/{id}/chat} surface.</li>
 * </ul>
 */
@Component
public final class ReservationChatChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_RESERVATION =
            Pattern.compile("^/topic/reservations/(\\d+)$");
    private static final Pattern APP_SEND_MESSAGE =
            Pattern.compile("^/app/reservations/(\\d+)/messages$");

    private static final String ROLE_ADMIN_AUTHORITY = UserRole.ADMIN.springAuthorityName();

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
        final Authentication authentication = authenticationOrNull(accessor.getUser());
        final long viewerUserId = resolveUserId(authentication);
        // SUBSCRIBE is a read-only operation: admins are allowed to audit any reservation chat.
        // SEND remains participant-only so the admin role cannot impersonate a rider/owner.
        if (command == StompCommand.SUBSCRIBE && hasAdminAuthority(authentication)) {
            return message;
        }
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

    @Nullable
    private static Authentication authenticationOrNull(@Nullable final Principal principal) {
        return principal instanceof Authentication auth ? auth : null;
    }

    private static long resolveUserId(@Nullable final Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof RydenUserDetails details) {
            return details.getUserId();
        }
        throw new AccessDeniedException("Unauthenticated WebSocket session");
    }

    private static boolean hasAdminAuthority(@Nullable final Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (final GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
