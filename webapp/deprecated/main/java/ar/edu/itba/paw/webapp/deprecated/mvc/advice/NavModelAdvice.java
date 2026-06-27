package ar.edu.itba.paw.webapp.deprecated.mvc.advice;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.util.WebAuthUtils;

/**
 * Adds authenticated user forename, surname, and optional profile picture id for layout navigation.
 */
@ControllerAdvice
public final class NavModelAdvice {

    private final UserService userService;
    private final ReservationService reservationService;

    public NavModelAdvice(final UserService userService, final ReservationService reservationService) {
        this.userService = userService;
        this.reservationService = reservationService;
    }

    @ModelAttribute
    public void addNavUserAttributes(final Model model) {
        WebAuthUtils.currentUserDetails(SecurityContextHolder.getContext().getAuthentication())
                .ifPresent(details -> {
                    model.addAttribute("navUserForename", details.getForename());
                    model.addAttribute("navUserSurname", details.getSurname());
                    userService.getUserById(details.getUserId())
                            .ifPresent(u -> {
                                u.getProfilePictureId()
                                        .ifPresent(id -> model.addAttribute("navProfilePictureImageId", id));
                                // Used by the global blocked-account banner rendered in navbar.tag, and
                                // by service-layer-guard messaging across owner-side pages (publish,
                                // my-cars detail, etc.). Re-read from DB so a freshly applied block by the
                                // overdue-refund sweep takes effect on the next request without re-login.
                                model.addAttribute("navUserBlocked", u.isBlocked());
                                if (u.isBlocked()) {
                                    addBlockedBannerDeepLinkAttributes(model, u.getId());
                                }
                            });
                });
    }

    /**
     * Decides what the blocked-banner CTA should point at. When exactly one reservation has an overdue
     * refund proof we expose its id so the CTA can deep-link to the detail page (where the upload button
     * lives). For zero (race with auto-unblock) or many, the CTA falls back to the owner reservations
     * list in navbar.tag — see that template for the conditional render.
     */
    private void addBlockedBannerDeepLinkAttributes(final Model model, final long ownerUserId) {
        final List<Long> overdueIds = reservationService.findOverdueRefundProofReservationIdsForOwner(ownerUserId);
        model.addAttribute("navBlockedOverdueCount", overdueIds.size());
        if (overdueIds.size() == 1) {
            model.addAttribute("navBlockedSingleReservationId", overdueIds.get(0));
        }
    }
}
