package ar.edu.itba.paw.persistence.reservation;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageProjection;

public interface ReservationMessageDao {

    ReservationMessage create(long reservationId, long senderUserId, String body);

    ReservationMessage create(long reservationId, long senderUserId, String body, Long attachmentFileId);

    List<ReservationMessageProjection> findProjectedByReservationIdOrderByCreatedAtAsc(
            long reservationId, int offset, int limit);

    List<ReservationMessageProjection> findProjectedByReservationIdAfterIdOrderByCreatedAtAsc(
            long reservationId, long afterMessageId, int limit);

    long countByReservationId(long reservationId);

    Optional<ReservationMessage> findByIdAndReservationId(long messageId, long reservationId);

    Optional<ReservationMessageProjection> findProjectedByIdAndReservationId(long messageId, long reservationId);

    List<ReservationMessage> findPendingEmailNotification(int limit);

    int markEmailNotified(Collection<Long> messageIds);

    int markSeenByRecipient(long reservationId, long recipientUserId);
}
