package ar.edu.itba.paw.services.user.view;

import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.profile.CounterpartyActiveListingsFragment;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;

import ar.edu.itba.paw.services.reservation.ReservationService;
/**
 * Read-only API for the "counterparty profile" page (header + recent reviews + other active
 * listings). Used in two scenarios that previously each had their own controller-level
 * orchestration:
 *
 * Public viewer browsing an owner's profile from the car-detail page.
 * Authenticated reservation participant opening their counterpart's profile from "My
 * reservations" (the access check requires viewer-is-participant in this case).
 *
 * The active-listings grid is also paginated via AJAX; the same service owns that fragment
 * loader so the criteria (sort, page size, excludeCarId) live in a single place.
 */
public interface CounterpartyProfileViewService {

    /**
     * Loads the public counterparty profile (no participant check). Use this for the "owner of
     * this car" link rendered on a car-detail page.
     *
     * @param counterpartyUserId id of the user whose profile is being viewed
     * @param currentCarId       optional: excluded from the "other active listings" grid so the
     *                           car the visitor came from is not shown twice
     * @param locale             locale for the "member since" formatted label
     * @return populated page model when the user exists; empty otherwise
     */
    Optional<CounterpartyProfilePageModel> loadPublicCounterpartyProfile(
            long counterpartyUserId, Long currentCarId, Locale locale);

    /**
     * Loads the counterparty profile linked to a reservation, when the viewer is a participant
     * (rider or owner). Centralizes both the participant-access check (via {@link
     * ReservationService}) and the page-model assembly.
     *
     * @param viewerUserId   id of the logged-in user opening the page
     * @param reservationId  reservation primary key
     * @param role           {@code "rider"} or {@code "owner"}: viewer role
     * @param locale         locale for formatted fields
     * @return profile projection when allowed; otherwise empty
     */
    Optional<CounterpartyProfilePageModel> loadCounterpartyProfileForReservationParticipant(
            long viewerUserId, long reservationId, String role, Locale locale);

    /**
     * Returns one page of the counterparty's "other active listings" grid for the AJAX "load
     * more" endpoint. The page size matches the one used for the initial render so consecutive
     * fragments line up.
     *
     * @param counterpartyUserId id of the user whose listings are being paged
     * @param excludeCarId       optional: car to exclude from the grid (e.g. the one the viewer
     *                           is currently looking at)
     * @param page               1-indexed page number; values {@code <= 0} return an empty fragment
     * @return fragment with the cards plus paging hints; empty fragment when the user does not
     *         exist or the page is out of range
     */
    Optional<CounterpartyActiveListingsFragment> loadCounterpartyActiveListingsPage(
            long counterpartyUserId, Long excludeCarId, int page);
}
