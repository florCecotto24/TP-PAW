package ar.edu.itba.paw.persistence;

import java.util.List;

import ar.edu.itba.paw.models.domain.ReservationMessage;

public interface ReservationMessageDao {

    ReservationMessage create(long reservationId, long senderUserId, String body);

    List<ReservationMessage> findByReservationIdOrderByCreatedAtAsc(long reservationId, int offset, int limit);

    long countByReservationId(long reservationId);
}
