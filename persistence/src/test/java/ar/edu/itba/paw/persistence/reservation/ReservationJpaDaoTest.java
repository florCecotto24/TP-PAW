package ar.edu.itba.paw.persistence.reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

/** Phase 1 coverage: refund-overdue queries that drive the owner-blocking sweep. */
class ReservationJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ReservationDao dao;

    @PersistenceContext
    private EntityManager em;

    private long ownerId;
    private long riderId;
    private long carId;

    @BeforeEach
    void seedCatalog() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "owner-r@test.com", "Owner", "Test");
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rider-r@test.com", "Rider", "Test");
        ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "owner-r@test.com");
        riderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "rider-r@test.com");

        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "BrandX", true);
        final long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "BrandX");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "ModelX", true, "HATCHBACK");
        final long modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "ModelX");
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, transmission, powertrain, status, "
                        + "model_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                ownerId, "RES001", "MANUAL", "GASOLINE", "active", modelId, now, now);
        carId = jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, "RES001");
    }

    @Test
    void testFindReservationsWithOverdueRefundProofReturnsOnlyPastDueWithoutReceipt() {
        // 1. Arrange — three reservations: overdue (no receipt), overdue (receipt uploaded), future deadline.
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        final long overdueWithoutReceiptId = insertReservation(
                "cancelled_by_owner", true, null,
                OffsetDateTime.parse("2026-05-30T00:00:00Z"));
        final long overdueWithReceiptId = insertReservation(
                "cancelled_by_owner", true, insertStoredFile("rcpt.pdf"),
                OffsetDateTime.parse("2026-05-30T00:00:00Z"));
        final long futureDeadlineId = insertReservation(
                "cancelled_by_owner", true, null,
                OffsetDateTime.parse("2026-06-10T00:00:00Z"));

        // 2. Act
        final List<Reservation> overdue = dao.findReservationsWithOverdueRefundProof(now);

        // 3. Assert — only the past-deadline, receipt-less, refund-required reservation comes back.
        final List<Long> ids = overdue.stream().map(Reservation::getId).toList();
        Assertions.assertTrue(ids.contains(overdueWithoutReceiptId));
        Assertions.assertFalse(ids.contains(overdueWithReceiptId));
        Assertions.assertFalse(ids.contains(futureDeadlineId));
    }

    @Test
    void testFindReservationsWithOverdueRefundProofExcludesNonCancelledStatuses() {
        // 1. Arrange — a 'finished' reservation with overdue deadline must not surface.
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        insertReservation(
                "finished", true, null,
                OffsetDateTime.parse("2026-05-29T00:00:00Z"));

        // 2. Act
        final List<Reservation> overdue = dao.findReservationsWithOverdueRefundProof(now);

        // 3. Assert
        Assertions.assertTrue(overdue.isEmpty());
    }

    @Test
    void testCountOverdueRefundProofsForOwnerCountsOnlyTrueOverdueRows() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        insertReservation("cancelled_by_owner", true, null,
                OffsetDateTime.parse("2026-05-30T00:00:00Z"));
        insertReservation("cancelled_by_rider", true, null,
                OffsetDateTime.parse("2026-05-31T00:00:00Z"));
        insertReservation("cancelled_by_owner", true, insertStoredFile("ok.pdf"),
                OffsetDateTime.parse("2026-05-30T00:00:00Z"));
        insertReservation("cancelled_by_owner", true, null,
                OffsetDateTime.parse("2026-06-10T00:00:00Z"));

        // 2. Act
        final long count = dao.countOverdueRefundProofsForOwner(ownerId, now);

        // 3. Assert — two past-deadline + receipt-less + refund-required rows.
        Assertions.assertEquals(2L, count);
    }

    @Test
    void testCountOverdueRefundProofsForOwnerReturnsZeroForUnrelatedOwner() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        insertReservation("cancelled_by_owner", true, null,
                OffsetDateTime.parse("2026-05-30T00:00:00Z"));

        // 2. Act
        final long count = dao.countOverdueRefundProofsForOwner(ownerId + 9999L, now);

        // 3. Assert
        Assertions.assertEquals(0L, count);
    }

    private long insertReservation(
            final String statusLower,
            final boolean refundRequired,
            final Long receiptFileId,
            final OffsetDateTime refundDeadline) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, car_id, start_date, end_date, status, "
                        + "total_price, created_at, updated_at, payment_refund_required, "
                        + "payment_refund_receipt_file_id, refund_proof_deadline_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                riderId, carId,
                LocalDate.of(2026, 4, 1).atStartOfDay(),
                LocalDate.of(2026, 4, 3).atStartOfDay(),
                statusLower, new BigDecimal("100.00"),
                now, now,
                refundRequired,
                receiptFileId,
                refundDeadline);
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM reservations WHERE car_id = ?", Long.class, carId);
    }

    private long insertStoredFile(final String name) {
        jdbcTemplate.update(
                "INSERT INTO stored_files (uploader_user_id, file_name, content_type, byte_array, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                ownerId, name, "application/pdf", new byte[] { 1, 2, 3 }, OffsetDateTime.now(ZoneOffset.UTC));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM stored_files WHERE file_name = ?", Long.class, name);
    }

    /** Quick reference to the {@link Car} so the foreign key references resolve. */
    @SuppressWarnings("unused")
    private Car loadCar() {
        return em.find(Car.class, carId);
    }
}
