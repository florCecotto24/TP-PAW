package ar.edu.itba.paw.persistence.reservation;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.function.Function;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.reservation.ReservationMessageProjection;

@Transactional(readOnly = true)
@Repository
public class ReservationMessageJpaDao implements ReservationMessageDao {

    private static final String MESSAGE_PROJECTION_SELECT =
            "SELECT m.id AS message_id, m.reservation_id AS reservation_id, "
                    + "u.id AS sender_user_id, u.forename AS sender_forename, "
                    + "u.surname AS sender_surname, m.body AS body, m.created_at AS created_at, "
                    + "m.seen AS seen, sf.id AS attachment_file_id, sf.file_name AS attachment_file_name, "
                    + "sf.content_type AS attachment_content_type, sf.size_bytes AS attachment_size_bytes "
                    + "FROM reservation_messages m "
                    + "JOIN users u ON u.id = m.sender_user_id "
                    + "LEFT JOIN stored_files sf ON sf.id = m.attachment_file_id ";

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
    public List<ReservationMessageProjection> findProjectedByReservationIdOrderByCreatedAtAsc(
            final long reservationId, final int offset, final int limit) {
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = em.createNativeQuery(
                        MESSAGE_PROJECTION_SELECT
                                + "WHERE m.reservation_id = :reservationId "
                                + "ORDER BY m.created_at ASC, m.id ASC "
                                + "LIMIT :limit OFFSET :offset")
                .setParameter("reservationId", reservationId)
                .setParameter("limit", Math.max(1, limit))
                .setParameter("offset", Math.max(0, offset))
                .getResultList();
        return rows.stream()
                .map(ReservationMessageJpaDao::toProjection)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReservationMessageProjection> findProjectedByReservationIdAfterIdOrderByCreatedAtAsc(
            final long reservationId, final long afterMessageId, final int limit) {
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = em.createNativeQuery(
                        MESSAGE_PROJECTION_SELECT
                                + "WHERE m.reservation_id = :reservationId AND m.id > :afterMessageId "
                                + "ORDER BY m.created_at ASC, m.id ASC "
                                + "LIMIT :limit")
                .setParameter("reservationId", reservationId)
                .setParameter("afterMessageId", Math.max(0L, afterMessageId))
                .setParameter("limit", Math.max(1, limit))
                .getResultList();
        return rows.stream()
                .map(ReservationMessageJpaDao::toProjection)
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
        final List<ReservationMessage> results = em.createQuery(
                        "FROM ReservationMessage m "
                                + "JOIN FETCH m.sender "
                                + "LEFT JOIN FETCH m.attachment "
                                + "WHERE m.id = :messageId AND m.reservation.id = :reservationId",
                        ReservationMessage.class)
                .setParameter("messageId", messageId)
                .setParameter("reservationId", reservationId)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<ReservationMessageProjection> findProjectedByIdAndReservationId(
            final long messageId, final long reservationId) {
        @SuppressWarnings("unchecked")
        final List<Object[]> rows = em.createNativeQuery(
                        MESSAGE_PROJECTION_SELECT
                                + "WHERE m.id = :messageId AND m.reservation_id = :reservationId "
                                + "LIMIT 1")
                .setParameter("messageId", messageId)
                .setParameter("reservationId", reservationId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(toProjection(rows.get(0)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ReservationMessage> findPendingEmailNotification(final int limit) {
        final int safeLimit = Math.max(1, Math.min(limit, 500));
        // No JOIN FETCH of attachment: StoredFile.data is effectively eager without bytecode
        // enhancement; digest only needs reservation/sender (+ optional filename via FK id).
        final List<Number> idRows = em.createNativeQuery(
                        "SELECT m.id FROM reservation_messages m "
                                + "WHERE m.email_notified = FALSE AND m.seen = FALSE "
                                + "ORDER BY m.created_at ASC, m.id ASC "
                                + "LIMIT :limit")
                .setParameter("limit", safeLimit)
                .getResultList();
        if (idRows.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = idRows.stream().map(Number::longValue).collect(Collectors.toList());
        final List<ReservationMessage> hydrated = em.createQuery(
                        "SELECT DISTINCT m FROM ReservationMessage m "
                                + "JOIN FETCH m.reservation "
                                + "JOIN FETCH m.sender "
                                + "WHERE m.id IN :ids",
                        ReservationMessage.class)
                .setParameter("ids", ids)
                .getResultList();
        final Map<Long, ReservationMessage> byId = hydrated.stream()
                .collect(Collectors.toMap(ReservationMessage::getId, Function.identity(), (a, b) -> a));
        final List<ReservationMessage> ordered = new ArrayList<>(ids.size());
        for (final Long id : ids) {
            final ReservationMessage message = byId.get(id);
            if (message != null) {
                ordered.add(message);
            }
        }
        return ordered;
    }

    @Override
    @Transactional
    public int markEmailNotified(final Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }
        final List<ReservationMessage> messages = em.createQuery(
                        "FROM ReservationMessage m "
                                + "WHERE m.id IN :ids AND m.emailNotified = false AND m.seen = false",
                        ReservationMessage.class)
                .setParameter("ids", messageIds)
                .getResultList();
        for (final ReservationMessage message : messages) {
            message.setEmailNotified(true);
        }
        return messages.size();
    }

    @Override
    @Transactional
    public int markSeenByRecipient(final long reservationId, final long recipientUserId) {
        final List<ReservationMessage> messages = em.createQuery(
                        "FROM ReservationMessage m "
                                + "WHERE m.reservation.id = :reservationId "
                                + "AND m.sender.id != :recipientUserId "
                                + "AND m.seen = false",
                        ReservationMessage.class)
                .setParameter("reservationId", reservationId)
                .setParameter("recipientUserId", recipientUserId)
                .getResultList();
        for (final ReservationMessage message : messages) {
            message.setSeen(true);
        }
        return messages.size();
    }

    private static ReservationMessageProjection toProjection(final Object[] row) {
        return new ReservationMessageProjection(
                asLong(row[0]),
                asLong(row[1]),
                asLong(row[2]),
                (String) row[3],
                (String) row[4],
                (String) row[5],
                asOffsetDateTime(row[6]),
                asBoolean(row[7]),
                asNullableLong(row[8]),
                (String) row[9],
                (String) row[10],
                asNullableLong(row[11]));
    }

    private static long asLong(final Object value) {
        return ((Number) value).longValue();
    }

    private static Long asNullableLong(final Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static boolean asBoolean(final Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return ((Number) value).intValue() != 0;
    }

    private static OffsetDateTime asOffsetDateTime(final Object value) {
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalArgumentException("Unsupported timestamp value: " + value);
    }
}
