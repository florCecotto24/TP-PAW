package ar.edu.itba.paw.webapp.security.access;

import java.util.OptionalLong;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.AuthenticationAuthorities;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.http.HttpRequestPathIds;

/**
 * Authorization helpers for {@code /my-reservations/**} (used with {@link AuthorizationManager} in security config).
 *
 * Roles considered:
 * <b>Participant</b> (rider or owner of the reservation): full read + write access.
 * <b>Admin</b> ({@link UserRole#ADMIN}): <em>read-only</em> access to the chat-related endpoints
 * so the admin reservation-chat view can reuse the same HTTP surface (messages REST list,
 * attachment download/view, chat HTML page). Admins are intentionally excluded from write
 * endpoints (POST messages, cancel, receipts, reviews) so the support role cannot impersonate
 * a participant.
 */
@Component("reservationWebAuth")
public final class ReservationWebAuthorization {

    private final ReservationService reservationService;

    public ReservationWebAuthorization(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public boolean isParticipant(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong ridOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-reservations/");
        if (!ridOpt.isPresent()) {
            return false;
        }
        final long rid = ridOpt.getAsLong();
        return reservationService.getRiderReservationById(userId, rid).isPresent()
                || reservationService.getOwnerReservationById(userId, rid).isPresent();
    }

    public boolean isRider(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong ridOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-reservations/");
        if (!ridOpt.isPresent()) {
            return false;
        }
        final long rid = ridOpt.getAsLong();
        return reservationService.getRiderReservationById(userId, rid).isPresent();
    }

    public boolean isOwner(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong ridOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-reservations/");
        if (!ridOpt.isPresent()) {
            return false;
        }
        final long rid = ridOpt.getAsLong();
        return reservationService.getOwnerReservationById(userId, rid).isPresent();
    }

    /**
     * Filter-side check for the rider-driven "edit reservation period" endpoints: the caller must be
     * the rider of the reservation, and the reservation must still be {@link Reservation.Status#PENDING}
     * with no attached payment receipt. Mirrors the service-side guard inside
     * {@code ReservationServiceImpl#editPendingReservationByRider} so we reject the request before
     * controller logic runs.
     */
    public boolean isRiderUnpaidPending(final Authentication authentication, final HttpServletRequest request) {
        final Long userId = authenticatedUserId(authentication);
        if (userId == null) {
            return false;
        }
        final OptionalLong ridOpt = HttpRequestPathIds.firstLongSegmentAfterPrefix(request, "/my-reservations/");
        if (!ridOpt.isPresent()) {
            return false;
        }
        final long rid = ridOpt.getAsLong();
        return reservationService.getRiderReservationById(userId, rid)
                .filter(r -> r.getStatus() == Reservation.Status.PENDING)
                .filter(r -> r.getPaymentReceiptFileId().isEmpty())
                .isPresent();
    }

    public AuthorizationManager<RequestAuthorizationContext> participantAccess() {
        return authenticatedManager(this::isParticipant);
    }

    public AuthorizationManager<RequestAuthorizationContext> riderAccess() {
        return authenticatedManager(this::isRider);
    }

    public AuthorizationManager<RequestAuthorizationContext> ownerAccess() {
        return authenticatedManager(this::isOwner);
    }

    /**
     * Access manager for endpoints that may only be exercised by the rider on a pending, still-unpaid
     * reservation (rider-side period edit). See {@link #isRiderUnpaidPending}.
     */
    public AuthorizationManager<RequestAuthorizationContext> riderUnpaidPendingAccess() {
        return authenticatedManager(this::isRiderUnpaidPending);
    }

    /**
     * Read-only access manager for chat-style endpoints: grants access to the reservation
     * participants or to any caller with the {@link UserRole#ADMIN} authority. Use this
     * for {@code GET}-only matchers so admins can audit conversations without being able to send
     * messages, cancel, upload receipts, or post reviews on behalf of a participant.
     */
    public AuthorizationManager<RequestAuthorizationContext> participantOrAdminReadAccess() {
        return authenticatedManager(
                (auth, request) -> AuthenticationAuthorities.hasAdminRole(auth) || isParticipant(auth, request));
    }

    private static AuthorizationManager<RequestAuthorizationContext> authenticatedManager(
            final ReservationAuthCheck check) {
        return (Supplier<Authentication> authentication, RequestAuthorizationContext context) -> {
            final Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(check.allow(auth, context.getRequest()));
        };
    }

    @FunctionalInterface
    private interface ReservationAuthCheck {
        boolean allow(Authentication authentication, HttpServletRequest request);
    }

    private static Long authenticatedUserId(final Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (!(authentication.getPrincipal() instanceof RydenUserDetails details)) {
            return null;
        }
        return details.getUserId();
    }
}
