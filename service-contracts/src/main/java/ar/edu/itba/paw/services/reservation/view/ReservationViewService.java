package ar.edu.itba.paw.services.reservation.view;


import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.reservation.CarReservationsListPageModel;
import ar.edu.itba.paw.models.dto.reservation.OwnerReservationsListPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.dto.reservation.ReservationCardDisplayRow;
import ar.edu.itba.paw.models.dto.reservation.ReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationDetailPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationEditPageModel;
import ar.edu.itba.paw.models.dto.reservation.RiderReservationsListPageModel;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;

import ar.edu.itba.paw.services.reservation.ReservationService;
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
     * Builds the page model for the rider-side "edit reservation period" form. Returns empty when
     * the viewer is not the rider on the reservation, when the reservation is no longer editable
     * (not {@link ar.edu.itba.paw.models.domain.reservation.Reservation.Status#PENDING} or already has a payment
     * receipt) or when the underlying car has been removed. The bookable-segments JSON is computed
     * with the current reservation excluded so its existing days remain selectable.
     */
    Optional<ReservationEditPageModel> loadRiderEditReservationPage(
            long riderUserId, long reservationId, Locale locale);

    /**
     * Loads the reservation chat page when the viewer is a participant and chat is available for the reservation.
     *
     * @param viewerUserId id of the logged-in user
     * @param reservationId reservation primary key
     * @param role {@code "rider"} or {@code "owner"}: viewer role for access checks
     * @param locale locale for formatted fields (reserved for future labels)
     * @return chat page projection when allowed and chat is open; otherwise empty
     */
    Optional<ReservationChatPageModel> loadReservationChatForParticipant(
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

    /**
     * Loads the {@code reservation/ownerReservations.jsp} page model used by both
     * {@code MyCarsController.ownerReservations} (unscoped) and
     * {@code MyCarsController.ownerReservationsForCar} (scoped to a single car).
     *
     * @param criteria        prebuilt {@link ReservationSearchCriteria} (controller still owns
     *                        query-param normalization through {@code buildReservationSearchCriteria})
     * @param selectedCarOrNull when present, set as {@code selectedCar} in the model so the JSP can
     *                          render the per-car breadcrumb
     * @param currentSort     normalized {@code ownerCurrentSort} (echoed back to the JSP's sort UI)
     * @param locale          used to format the per-row display strings via
     *                        {@link #toReservationCardDisplayRow(ReservationCard, Locale)}
     */
    OwnerReservationsListPageModel loadOwnerReservationsListPage(
            ReservationSearchCriteria criteria,
            Car selectedCarOrNull,
            String currentSort,
            Locale locale);

    /**
     * Loads the {@code car/carReservations.jsp} page model used by
     * {@code MyCarsController.carReservations}.
     *
     * @param ownerUserId    authenticated owner id (used by the underlying
     *                       {@code getCarReservationCards} guard)
     * @param car            target car (already resolved + ownership-checked by the controller)
     * @param page           zero-based page index requested by the user
     * @param pageSize       controller-supplied page size (from {@code AppPaginationProperties})
     * @param statusFilter   normalized status token from
     *                       {@link #normalizeReservationStatusQueryParam(String)}; may be {@code null}
     * @param locale         locale used to format the per-row display strings
     */
    CarReservationsListPageModel loadCarReservationsListPage(
            long ownerUserId,
            Car car,
            int page,
            int pageSize,
            String statusFilter,
            Locale locale);

    /**
     * Loads the {@code reservation/myReservations.jsp} page model used by
     * {@code MyReservationsController.myReservations}. Owns the
     * {@code getRiderReservationCards + map(toReservationCardDisplayRow)} sequence so the
     * controller only has to forward query params and render the view.
     *
     * @param criteria    prebuilt {@link ReservationSearchCriteria} (controller still owns the
     *                    query-param normalisation through {@code buildReservationSearchCriteria})
     * @param currentSort normalised sort echoed back to the JSP
     * @param locale      used to format per-row display strings via
     *                    {@link #toReservationCardDisplayRow(ReservationCard, Locale)}
     */
    RiderReservationsListPageModel loadRiderReservationsListPage(
            ReservationSearchCriteria criteria, String currentSort, Locale locale);
}
