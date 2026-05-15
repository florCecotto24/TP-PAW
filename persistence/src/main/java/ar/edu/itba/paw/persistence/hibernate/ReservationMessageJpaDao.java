package ar.edu.itba.paw.persistence.hibernate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReservationMessageDao;

@Transactional
@Repository
public class ReservationMessageJpaDao implements ReservationMessageDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ReservationMessage create(final long reservationId, final long senderUserId, final String body) {
        final Reservation reservationRef = em.getReference(Reservation.class, reservationId);
        final User senderRef = em.getReference(User.class, senderUserId);
        final ReservationMessage message =
                new ReservationMessage(reservationRef, senderRef, body, OffsetDateTime.now(ZoneOffset.UTC));
        em.persist(message);
        return message;
    }

    @Override
    public List<ReservationMessage> findByReservationIdOrderByCreatedAtAsc(
            final long reservationId, final int offset, final int limit) {
        return em.createQuery(
                        "FROM ReservationMessage m WHERE m.reservation.id = :reservationId ORDER BY m.createdAt ASC",
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
}
