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

import ar.edu.itba.paw.services.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;
import ar.edu.itba.paw.webapp.security.http.HttpRequestPathIds;

/**
 * Authorization helpers for {@code /my-reservations/**} (used with {@link AuthorizationManager} in security config).
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

    public AuthorizationManager<RequestAuthorizationContext> participantAccess() {
        return authenticatedManager(this::isParticipant);
    }

    public AuthorizationManager<RequestAuthorizationContext> riderAccess() {
        return authenticatedManager(this::isRider);
    }

    public AuthorizationManager<RequestAuthorizationContext> ownerAccess() {
        return authenticatedManager(this::isOwner);
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
