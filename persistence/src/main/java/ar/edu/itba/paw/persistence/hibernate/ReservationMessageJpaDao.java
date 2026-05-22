package ar.edu.itba.paw.persistence.hibernate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReservationMessageDao;

@Transactional
@Repository
public class ReservationMessageJpaDao implements ReservationMessageDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ReservationMessage create(final long reservationId, final long senderUserId, final String body) {
        return create(reservationId, senderUserId, body, null);
    }

    @Override
    public ReservationMessage create(
            final long reservationId,
            final long senderUserId,
            final String body,
            final Long attachmentFileId) {
        final Reservation reservationRef = em.getReference(Reservation.class, reservationId);
        final User senderRef = em.getReference(User.class, senderUserId);
        final StoredFile attachmentRef =
                attachmentFileId == null ? null : em.getReference(StoredFile.class, attachmentFileId);
        final ReservationMessage message = new ReservationMessage(
                reservationRef, senderRef, body, attachmentRef, OffsetDateTime.now(ZoneOffset.UTC));
        em.persist(message);
        return message;
    }

    @Override
    public List<ReservationMessage> findByReservationIdOrderByCreatedAtAsc(
            final long reservationId, final int offset, final int limit) {
        return em.createQuery(
                        "SELECT m FROM ReservationMessage m "
                                + "LEFT JOIN FETCH m.attachment "
                                + "WHERE m.reservation.id = :reservationId ORDER BY m.createdAt ASC",
                        ReservationMessage.class)
                .setParameter("reservationId", reservationId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public long countByReservationId(final long reservationId) {
        return em.createQuery(
                        "SELECT COUNT(m) FROM ReservationMessage m WHERE m.reservation.id = :reservationId",
                        Long.class)
                .setParameter("reservationId", reservationId)
                .getSingleResult();
    }

    @Override
    public Optional<ReservationMessage> findByIdAndReservationId(final long messageId, final long reservationId) {
        final List<ReservationMessage> results = em.createQuery(
                        "SELECT m FROM ReservationMessage m "
                                + "LEFT JOIN FETCH m.attachment "
                                + "WHERE m.id = :messageId AND m.reservation.id = :reservationId",
                        ReservationMessage.class)
                .setParameter("messageId", messageId)
                .setParameter("reservationId", reservationId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
