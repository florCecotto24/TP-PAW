package ar.edu.itba.paw.persistence;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.persistence.ReservationMessageDao;

@Transactional(readOnly = true)
@Repository
public class ReservationMessageJpaDao implements ReservationMessageDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public ReservationMessage create(final long reservationId, final long senderUserId, final String body) {
        return create(reservationId, senderUserId, body, null);
    }

    @Override
    @Transactional
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
        /*
         * 1+1 query pattern (project rule: no LIMIT/OFFSET on JPQL):
         * - Step 1 (native): paginate + order on the {@code reservation_messages} table, projecting
         *   only the message PKs for the page window.
         * - Step 2 (JPQL):   hydrate the {@link ReservationMessage} entities by id with
         *   {@code LEFT JOIN FETCH m.attachment}. The reorder by {@code createdAt ASC} happens in
         *   memory because {@code WHERE id IN :ids} does not preserve native-step ordering.
         */
        @SuppressWarnings("unchecked")
        final List<Number> pageIds = em.createNativeQuery(
                        "SELECT m.id FROM reservation_messages m "
                                + "WHERE m.reservation_id = :reservationId "
                                + "ORDER BY m.created_at ASC, m.id ASC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("reservationId", reservationId)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();
        if (pageIds.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Long> ids = pageIds.stream().map(Number::longValue).collect(Collectors.toList());
        final List<ReservationMessage> hydrated = em.createQuery(
                        "FROM ReservationMessage m "
                                + "LEFT JOIN FETCH m.attachment "
                                + "WHERE m.id IN :ids",
                        ReservationMessage.class)
                .setParameter("ids", ids)
                .getResultList();
        return hydrated.stream()
                .sorted(Comparator.comparing(ReservationMessage::getCreatedAt)
                        .thenComparingLong(ReservationMessage::getId))
                .collect(Collectors.toList());
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
        /*
         * Not pagination: the WHERE clause filters by {@code m.id} (the message PK), which
         * guarantees at most one row by definition. The extra equality on {@code reservation.id}
         * is just an authorization scope check. The trailing {@code setMaxResults(1)} is purely
         * a defensive cap and does NOT count as JPQL-level pagination under the 1+1 rule.
         */
        final List<ReservationMessage> results = em.createQuery(
                        "FROM ReservationMessage m "
                                + "LEFT JOIN FETCH m.attachment "
                                + "WHERE m.id = :messageId AND m.reservation.id = :reservationId",
                        ReservationMessage.class)
                .setParameter("messageId", messageId)
                .setParameter("reservationId", reservationId)
                .setMaxResults(1)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<ReservationMessage> findPendingEmailNotification() {
        return em.createQuery(
                        "SELECT DISTINCT m FROM ReservationMessage m "
                                + "JOIN FETCH m.reservation "
                                + "JOIN FETCH m.sender "
                                + "LEFT JOIN FETCH m.attachment "
                                + "WHERE m.emailNotified = false "
                                + "ORDER BY m.createdAt ASC",
                        ReservationMessage.class)
                .getResultList();
    }

    @Override
    @Transactional
    public int markEmailNotified(final Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }
        final List<ReservationMessage> messages = em.createQuery(
                        "FROM ReservationMessage m WHERE m.id IN :ids AND m.emailNotified = false",
                        ReservationMessage.class)
                .setParameter("ids", messageIds)
                .getResultList();
        for (final ReservationMessage message : messages) {
            message.setEmailNotified(true);
        }
        return messages.size();
    }
}
