package ar.edu.itba.paw.services;

import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.dto.ReservationCard;
import ar.edu.itba.paw.models.dto.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.profile.CounterpartyProfilePageModel;

/**
 * Read-only API for reservation-related UI data: hub list cards, reservation detail, and counterparty profile views.
 * <p>
 * Domain rules, persistence, and mutating workflows remain on {@link ReservationService}; this type only loads data
 * and builds presentation DTOs for controllers and JSPs.
 */
public interface ReservationViewService {

    /**
     * Loads a reservation detail page for the authenticated participant (rider or owner), if they may view it.
     *
     * @param viewerUserId id of the logged-in user opening the page
     * @param reservationId reservation primary key
     * @param role          {@code "rider"} or {@code "owner"}: which hat the viewer wears for this page
     * @param locale        locale for formatted dates and labels
     * @return populated model when the reservation exists, the viewer is allowed, and listing detail is available; otherwise empty
     */
    Optional<ReservationDetailPageModel> loadMyReservationDetailForViewer(
            long viewerUserId, long reservationId, String role, Locale locale);

    /**
     * Loads the counterparty profile linked to a reservation (the other party: owner or rider), when the viewer is a participant.
     *
     * @param viewerUserId id of the logged-in user
     * @param reservationId reservation primary key
     * @param role          {@code "rider"} or {@code "owner"}: viewer role for access checks
     * @param locale        locale for formatted fields
     * @return profile projection when allowed; otherwise empty
     */
    Optional<CounterpartyProfilePageModel> loadCounterpartyProfileForReservationParticipant(
            long viewerUserId, long reservationId, String role, Locale locale);

    /**
     * Maps a compact {@link ReservationCard} (as returned by search/list APIs) into a row DTO for hub list rendering.
     *
     * @param card   reservation summary including listing and date range
     * @param locale locale for pickup/return timestamps
     * @return display row (status key, formatted money, etc.); never {@code null}
     */
    ReservationCardDisplayRow toReservationCardDisplayRow(ReservationCard card, Locale locale);

    /**
     * Normalizes a single reservation status query parameter from the hub filter (trim, case-fold, whitelist).
     *
     * @param raw raw query value; may be null or blank
     * @return canonical lowercase status token when whitelisted; otherwise {@code null}
     */
    String normalizeReservationStatusQueryParam(String raw);
}
