package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;



import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.ReservationCard;

public interface ReservationDao {

    boolean hasActiveOverlap(long listingId, OffsetDateTime startDate, OffsetDateTime endDate);

    List<Reservation> findBlockingByListingId(long listingId);

    List<Reservation> findBlockingByListingIds(Collection<Long> listingIds);

    Reservation createReservation(
            long riderId,
            long listingId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Reservation.Status status,
            BigDecimal totalPrice,
            OffsetDateTime paymentProofDeadlineAt);

    List<Reservation> findPendingPaymentPastDeadline(OffsetDateTime now);

    int attachPaymentReceiptAndAccept(long reservationId, long riderId, long storedFileId);

    int updatePaymentApproved(long reservationId, long ownerUserId, boolean approved);

    Optional<Reservation> getReservationById(long id);

    Optional<Reservation> getOwnerReservationById(long ownerId, long reservationId);

    Page<ReservationCard> getRiderReservationCards(long riderId, int page, int pageSize, String statusFilter);

    List<Reservation> getReminderReservations(final OffsetDateTime from, final OffsetDateTime to);

    Page<ReservationCard> getOwnerReservationCards(long ownerId, int page, int pageSize, String statusFilter);

    int updateReservationStatus(long reservationId, String status);

    List<Reservation> getListingActiveReservations(long listingId);

    Page<ReservationCard> getListingReservationCards(long ownerId, long listingId, int page, int pageSize, String statusFilter);

    Map<String, Long> countListingReservationsByStatus(long ownerId, long listingId);

    BigDecimal sumListingRevenueByStatuses(long ownerId, long listingId, Collection<String> statuses);

    long countListingReservationsCreatedBetween(long ownerId, long listingId, OffsetDateTime from, OffsetDateTime until);

    Optional<OffsetDateTime> findListingNextActiveReservationDate(long ownerId, long listingId, OffsetDateTime after);

    List<Reservation> findListingFinishedReservations(long ownerId, long listingId);
}
