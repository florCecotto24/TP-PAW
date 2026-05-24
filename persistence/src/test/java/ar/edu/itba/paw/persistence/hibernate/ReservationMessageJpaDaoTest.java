package ar.edu.itba.paw.persistence.hibernate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import ar.edu.itba.paw.persistence.ReservationMessageDao;

class ReservationMessageJpaDaoTest extends DaoIntegrationTestSupport {

    private static final String BODY = "Hello from the rider";

    @Autowired
    private ReservationMessageDao dao;

    @PersistenceContext
    private EntityManager em;

    private long reservationId;
    private long riderId;

    @BeforeEach
    void seedReservation() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "owner-chat@test.com",
                "Owner",
                "Test");
        final long ownerId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, "owner-chat@test.com");

        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rider-chat@test.com",
                "Rider",
                "Test");
        riderId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, "rider-chat@test.com");

        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, brand, model, type, transmission, powertrain) VALUES (?, ?, ?, ?, ?, ?, ?)",
                ownerId,
                "CHAT01",
                "Brand",
                "Model",
                "sedan",
                "manual",
                "gasoline");
        final long carId = jdbcTemplate.queryForObject("SELECT id FROM cars WHERE plate = ?", Long.class, "CHAT01");

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, car_id, start_date, end_date, status, total_price, created_at, "
                        + "updated_at, payment_approved) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                riderId,
                carId,
                now.plusDays(1),
                now.plusDays(3),
                "accepted",
                200.00,
                now,
                now,
                true);
        reservationId = jdbcTemplate.queryForObject(
                "SELECT id FROM reservations WHERE rider_id = ?", Long.class, riderId);
    }

    @Test
    void testCreatePersistsMessageWithTimestamp() {
        // 1.Arrange / 2.Exercise
        final ReservationMessage created = dao.create(reservationId, riderId, BODY);
        em.flush();

        // 3.Assert
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
    void testFindByReservationIdOrderByCreatedAtAscReturnsInsertedRows() {
        // 1.Arrange
        dao.create(reservationId, riderId, "First");
        dao.create(reservationId, riderId, "Second");
        em.flush();

        // 2.Exercise
        final var messages = dao.findByReservationIdOrderByCreatedAtAsc(reservationId, 0, 10);

        // 3.Assert
        Assertions.assertEquals(2, messages.size());
        Assertions.assertEquals("First", messages.get(0).getBody());
        Assertions.assertEquals("Second", messages.get(1).getBody());
    }

    @Test
    void testCreateWithAttachmentPersistsForeignKey() {
        // 1.Arrange
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO stored_files (uploader_user_id, file_name, content_type, byte_array, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                riderId,
                "photo.png",
                "image/png",
                new byte[] {1, 2, 3},
                now);
        final long fileId =
                jdbcTemplate.queryForObject("SELECT id FROM stored_files WHERE file_name = ?", Long.class, "photo.png");

        // 2.Exercise
        final ReservationMessage created = dao.create(reservationId, riderId, "", fileId);
        em.flush();

        // 3.Assert
        final Long attachmentId = jdbcTemplate.queryForObject(
                "SELECT attachment_file_id FROM reservation_messages WHERE id = ?",
                Long.class,
                created.getId());
        Assertions.assertEquals(fileId, attachmentId.longValue());
    }
}
