package ar.edu.itba.paw.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.ReservationMessage;

public interface ReservationMessageDao {

    ReservationMessage create(long reservationId, long senderUserId, String body);

    ReservationMessage create(long reservationId, long senderUserId, String body, Long attachmentFileId);

    List<ReservationMessage> findByReservationIdOrderByCreatedAtAsc(long reservationId, int offset, int limit);

    List<ReservationMessage> findByReservationIdAfterIdOrderByCreatedAtAsc(
            long reservationId, long afterMessageId, int limit);

    long countByReservationId(long reservationId);

    Optional<ReservationMessage> findByIdAndReservationId(long messageId, long reservationId);

    List<ReservationMessage> findPendingEmailNotification();

    int markEmailNotified(Collection<Long> messageIds);
}
