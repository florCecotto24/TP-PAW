package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.util.search.ReservationSearchCriteria;

/**
 * Read-only reservation lookups: by id (with optional participant scoping), paginated
 * "my reservations" card lists for rider / owner / car / admin, search-criteria sanitisation,
 * and the blocking / refund-overdue lookups consumed by other services and views.
 *
 * <p>The mutating side of the lifecycle lives in {@link ReservationWorkflowService}; the
 * money side in {@link ReservationPaymentService}; the per-car analytics in
 * {@link ReservationLifecycleSchedulerService}.
 */
public interface ReservationQueryService {

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getRiderReservationById(long riderId, long reservationId);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(ReservationSearchCriteria criteria);

    Page<ReservationCard> getOwnerReservationCards(ReservationSearchCriteria criteria);

    /**
     * Builds {@link ReservationSearchCriteria} from list/search parameters (filters, sort,
     * page index and page size supplied by the caller). Pass {@code carId} to restrict to a
     * single car (owner hub); pass {@code null} for all cars.
     */
    ReservationSearchCriteria buildReservationSearchCriteria(
            Long ownerId,
            Long riderId,
            List<Car.Type> category,
            List<Car.Transmission> transmission,
            List<Car.Powertrain> powertrain,
            BigDecimal priceMin,
            BigDecimal priceMax,
            List<String> rating,
            List<Reservation.Status> statusFilter,
            int page,
            int pageSize,
            String sort,
            String textQuery,
            Long carId);

    /** Owner car detail: paginated reservation cards for one car, optional status filter token. */
    Page<ReservationCard> getCarReservationCards(long ownerId, long carId, int page, int pageSize, String statusFilter);

    /** Reservations in {@code pending}, {@code accepted}, or {@code started} for one car. */
    List<Reservation> findBlockingReservationsByCarId(long carId);

    /** Same as {@link #findBlockingReservationsByCarId(long)} but excludes one reservation id. */
    List<Reservation> findBlockingReservationsByCarIdExcluding(long carId, long excludingReservationId);

    /** Blocking reservations for {@code carId} whose date range intersects {@code [from, to)} (UTC). */
    List<Reservation> findBlockingReservationsByCarIdInRange(long carId, OffsetDateTime from, OffsetDateTime to);

    /** Reservations whose pickup {@code start_date} lies in {@code [from, to)} (UTC). */
    List<Reservation> findReminderReservations(OffsetDateTime from, OffsetDateTime to);

    /** Admin-only: paginated list of every reservation in the system as display cards. */
    Page<ReservationCard> findAllReservationCards(int page, int pageSize);

    /**
     * Identifiers of reservations whose refund-proof deadline has lapsed for {@code ownerUserId}.
     * Ordered by deadline ascending (most overdue first).
     */
    List<Long> findOverdueRefundProofReservationIdsForOwner(long ownerUserId);

    /** Reservation ids belonging to {@code ownerUserId} that still require a refund proof upload. */
    Set<Long> findOwnerReservationIdsRequiringRefundProof(long ownerUserId);

    /** Car ids of {@code ownerUserId} that have at least one reservation still requiring a refund proof. */
    Set<Long> findOwnerCarIdsWithReservationRequiringRefundProof(long ownerUserId);
}
