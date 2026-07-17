package ar.edu.itba.paw.persistence.reservation;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.persistence.reservation.ReservationMessageDao;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class ReservationMessageJpaDaoTest extends DaoIntegrationTestSupport {

    private static final String BODY = "Hello from the rider";

    @Autowired
    private ReservationMessageDao dao;

    @PersistenceContext
    private EntityManager em;

    private long reservationId;
    private long ownerId;
    private long riderId;

    @BeforeEach
    void seedReservation() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "owner-chat@test.com",
                "Owner",
                "Test");
        ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "owner-chat@test.com");

        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rider-chat@test.com",
                "Rider",
                "Test");
        riderId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, "rider-chat@test.com");

        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, transmission, powertrain) VALUES (?, ?, ?, ?)",
                ownerId,
                "CHAT01",
                "manual",
                "gasoline");
        final long carId = jdbcTemplate.queryForObject("SELECT id FROM cars WHERE plate = ?", Long.class, "CHAT01");

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, car_id, start_date, end_date, status, total_price, created_at, "
                        + "updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                riderId,
                carId,
                now.plusDays(1),
                now.plusDays(3),
                "accepted",
                200.00,
                now,
                now);
        reservationId = jdbcTemplate.queryForObject(
                "SELECT id FROM reservations WHERE rider_id = ?", Long.class, riderId);
    }

    @Test
    void testCreatePersistsMessageWithTimestamp() {
        // 1. Arrange / 2. Act
        final ReservationMessage created = dao.create(reservationId, riderId, BODY);
        em.flush();

        // 3. Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT body, sender_user_id, reservation_id FROM reservation_messages WHERE id = ?",
                created.getId());
        Assertions.assertEquals(BODY, row.get("body"));
        Assertions.assertEquals(riderId, ((Number) row.get("sender_user_id")).longValue());
        Assertions.assertEquals(reservationId, ((Number) row.get("reservation_id")).longValue());
        Assertions.assertNotNull(
                jdbcTemplate.queryForObject(
                        "SELECT created_at FROM reservation_messages WHERE id = ?", Object.class, created.getId()));
    }

    @Test
    void testFindProjectedByReservationIdAfterIdOrderByCreatedAtAscReturnsOnlyNewerRows() {
        // 1. Arrange — seed three messages via JdbcTemplate so the DAO under test is only exercised in Act.
        final long firstId = insertMessage(reservationId, riderId, "First");
        insertMessage(reservationId, riderId, "Second");
        insertMessage(reservationId, riderId, "Third");

        // 2. Act
        final var messages =
                dao.findProjectedByReservationIdAfterIdOrderByCreatedAtAsc(reservationId, firstId, 10);

        // 3. Assert
        Assertions.assertEquals(2, messages.size());
        Assertions.assertEquals("Second", messages.get(0).getBody());
        Assertions.assertEquals("Third", messages.get(1).getBody());
    }

    @Test
    void testFindProjectedByReservationIdOrderByCreatedAtAscReturnsInsertedRows() {
        // 1. Arrange — seed via JdbcTemplate.
        insertMessage(reservationId, riderId, "First");
        insertMessage(reservationId, riderId, "Second");

        // 2. Act
        final var messages = dao.findProjectedByReservationIdOrderByCreatedAtAsc(reservationId, 0, 10);

        // 3. Assert
        Assertions.assertEquals(2, messages.size());
        Assertions.assertEquals("First", messages.get(0).getBody());
        Assertions.assertEquals("Second", messages.get(1).getBody());
    }

    @Test
    void testCreateWithAttachmentPersistsForeignKey() {
        // 1. Arrange — seed a stored file directly; the DAO call we test is the one in Act.
        final long fileId = insertStoredFile("photo.png", "image/png", new byte[] {1, 2, 3});

        // 2. Act
        final ReservationMessage created = dao.create(reservationId, riderId, "", fileId);
        em.flush();

        // 3. Assert
        final Long attachmentId = jdbcTemplate.queryForObject(
                "SELECT attachment_file_id FROM reservation_messages WHERE id = ?",
                Long.class,
                created.getId());
        Assertions.assertEquals(fileId, attachmentId.longValue());
    }

    @Test
    void testFindProjectedByReservationIdReturnsAttachmentMetadata() {
        // 1. Arrange — metadata projection must not require hydrating StoredFile.data.
        final long fileId = insertStoredFile("receipt.pdf", "application/pdf", new byte[] {1, 2, 3, 4});
        dao.create(reservationId, riderId, "", fileId);
        em.flush();

        // 2. Act
        final var messages = dao.findProjectedByReservationIdOrderByCreatedAtAsc(reservationId, 0, 10);

        // 3. Assert
        Assertions.assertEquals(1, messages.size());
        Assertions.assertEquals(fileId, messages.get(0).getAttachmentFileId());
        Assertions.assertEquals("receipt.pdf", messages.get(0).getAttachmentFileName());
        Assertions.assertEquals("application/pdf", messages.get(0).getAttachmentContentType());
        Assertions.assertEquals(4L, messages.get(0).getAttachmentSizeBytes());
        Assertions.assertEquals(riderId, messages.get(0).getSenderUserId());
        Assertions.assertEquals("Rider", messages.get(0).getSenderForename());
    }

    @Test
    void testFindPendingEmailNotificationReturnsOnlyUnnotifiedUnseenMessages() {
        // 1. Arrange — three messages: one pending, one already notified, one already seen.
        final long pendingId = insertMessage(reservationId, riderId, "Pending");
        insertMessageWithFlags(reservationId, riderId, "Notified", true, false);
        insertMessageWithFlags(reservationId, riderId, "Seen", false, true);

        // 2. Act
        final var pendingMessages = dao.findPendingEmailNotification(200);

        // 3. Assert
        Assertions.assertEquals(1, pendingMessages.size());
        Assertions.assertEquals(pendingId, pendingMessages.get(0).getId());
        Assertions.assertEquals("Pending", pendingMessages.get(0).getBody());
    }

    @Test
    void testMarkEmailNotifiedPersistsFlag() {
        // 1. Arrange
        final long messageId = insertMessage(reservationId, riderId, BODY);

        // 2. Act
        final int marked = dao.markEmailNotified(List.of(messageId));
        em.flush();

        // 3. Assert
        final Boolean notified = jdbcTemplate.queryForObject(
                "SELECT email_notified FROM reservation_messages WHERE id = ?", Boolean.class, messageId);
        Assertions.assertEquals(1, marked);
        Assertions.assertTrue(notified);
    }

    @Test
    void testMarkEmailNotifiedSkipsSeenMessages() {
        // 1. Arrange — message inserted with seen=TRUE so markEmailNotified must ignore it.
        final long messageId = insertMessageWithFlags(reservationId, riderId, BODY, false, true);

        // 2. Act
        final int marked = dao.markEmailNotified(List.of(messageId));
        em.flush();

        // 3. Assert
        final Boolean notified = jdbcTemplate.queryForObject(
                "SELECT email_notified FROM reservation_messages WHERE id = ?", Boolean.class, messageId);
        Assertions.assertEquals(0, marked);
        Assertions.assertFalse(notified);
    }

    @Test
    void testFindPendingEmailNotificationExcludesSeenMessagesAfterRecipientRead() {
        // 1. Arrange — message from rider already marked as seen (the state markSeenByRecipient would leave).
        insertMessageWithFlags(reservationId, riderId, "Unread until owner opens chat", false, true);

        // 2. Act
        final var pendingMessages = dao.findPendingEmailNotification(200);

        // 3. Assert
        Assertions.assertTrue(pendingMessages.isEmpty());
    }

    @Test
    void testMarkSeenByRecipientMarksOnlyCounterpartyMessages() {
        // 1. Arrange — one message from rider and one from owner.
        final long fromRiderId = insertMessage(reservationId, riderId, "From rider");
        final long fromOwnerId = insertMessage(reservationId, ownerId, "From owner");

        // 2. Act
        final int marked = dao.markSeenByRecipient(reservationId, ownerId);
        em.flush();

        // 3. Assert
        final Boolean riderMessageSeen = jdbcTemplate.queryForObject(
                "SELECT seen FROM reservation_messages WHERE id = ?", Boolean.class, fromRiderId);
        final Boolean ownerMessageSeen = jdbcTemplate.queryForObject(
                "SELECT seen FROM reservation_messages WHERE id = ?", Boolean.class, fromOwnerId);
        Assertions.assertEquals(1, marked);
        Assertions.assertTrue(riderMessageSeen);
        Assertions.assertFalse(ownerMessageSeen);
    }

    private long insertMessage(final long reservationId, final long senderUserId, final String body) {
        return insertMessageWithFlags(reservationId, senderUserId, body, false, false);
    }

    private long insertStoredFile(final String fileName, final String contentType, final byte[] data) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO stored_files (uploader_user_id, file_name, content_type, byte_array, size_bytes, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                riderId,
                fileName,
                contentType,
                data,
                (long) data.length,
                now);
        return jdbcTemplate.queryForObject("SELECT id FROM stored_files WHERE file_name = ?", Long.class, fileName);
    }

    private long insertMessageWithFlags(
            final long reservationId,
            final long senderUserId,
            final String body,
            final boolean emailNotified,
            final boolean seen) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservation_messages "
                        + "(reservation_id, sender_user_id, body, created_at, email_notified, seen) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                reservationId, senderUserId, body, now, emailNotified, seen);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation_messages "
                        + "WHERE reservation_id = ? AND sender_user_id = ? AND body = ? "
                        + "ORDER BY id DESC LIMIT 1",
                Long.class, reservationId, senderUserId, body);
    }
}
