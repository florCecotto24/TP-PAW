package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationCard;

public interface ReservationDao {

    boolean hasActiveOverlap(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Reservation> findBlockingByListingId(long listingId);

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            BigDecimal totalPrice);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(long riderId, int page, int pageSize);

    List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to);

    Page<ReservationCard> getOwnerReservationCards(long ownerId, int page, int pageSize);

    int updateReservationStatus(long reservationId, String status);
}
