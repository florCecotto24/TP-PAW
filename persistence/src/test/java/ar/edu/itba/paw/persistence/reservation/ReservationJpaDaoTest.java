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
import ar.edu.itba.paw.models.domain.file.StoredFile;
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

    @Test
    void testExistsByRiderIdAndCarIdReturnsTrueWhenRiderHasReservation() {
        // 1. Arrange
        insertReservation("accepted", false, null, null);

        // 2. Act
        final boolean exists = dao.existsByRiderIdAndCarId(riderId, carId);

        // 3. Assert
        Assertions.assertTrue(exists);
    }

    @Test
    void testExistsByRiderIdAndCarIdReturnsFalseForUnrelatedRider() {
        // 1. Arrange
        insertReservation("accepted", false, null, null);

        // 2. Act
        final boolean exists = dao.existsByRiderIdAndCarId(riderId + 9999L, carId);

        // 3. Assert
        Assertions.assertFalse(exists);
    }

    @Test
    void testFindAcceptedReservationIdsWithStartOnOrBeforeReturnsOnlyDueAccepted() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long dueAcceptedId = createAcceptedReservation(
                OffsetDateTime.parse("2026-06-10T10:00:00Z"),
                OffsetDateTime.parse("2026-06-15T18:00:00Z"));
        final long futureAcceptedId = createAcceptedReservation(
                OffsetDateTime.parse("2026-06-11T10:00:00Z"),
                OffsetDateTime.parse("2026-06-15T18:00:00Z"));
        createAcceptedReservation(
                OffsetDateTime.parse("2026-06-09T10:00:00Z"),
                OffsetDateTime.parse("2026-06-12T18:00:00Z"),
                Reservation.Status.STARTED);
        em.flush();
        em.clear();

        // 2. Act
        final List<Long> ids = dao.findAcceptedReservationIdsWithStartOnOrBefore(now);

        // 3. Assert
        Assertions.assertEquals(List.of(dueAcceptedId), ids);
        Assertions.assertFalse(ids.contains(futureAcceptedId));
    }

    @Test
    void testTransitionAcceptedToStartedIfDueUpdatesWhenPickupReached() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long reservationId = createAcceptedReservation(
                OffsetDateTime.parse("2026-06-10T10:00:00Z"),
                OffsetDateTime.parse("2026-06-15T18:00:00Z"));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.transitionAcceptedToStartedIfDue(reservationId, now);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(
                "started",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
    }

    @Test
    void testTransitionAcceptedToStartedIfDueNoOpWhenPickupStillFuture() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long reservationId = createAcceptedReservation(
                OffsetDateTime.parse("2026-06-11T10:00:00Z"),
                OffsetDateTime.parse("2026-06-15T18:00:00Z"));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.transitionAcceptedToStartedIfDue(reservationId, now);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(0, updated);
        Assertions.assertEquals(
                "accepted",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
    }

    @Test
    void testApplyAdminCarPauseCancellationMarksAcceptedAsCancelledByOwner() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long receiptId = insertStoredFile("paid.pdf");
        final long reservationId = createAcceptedReservation(
                OffsetDateTime.parse("2026-06-15T10:00:00Z"),
                OffsetDateTime.parse("2026-06-20T18:00:00Z"));
        jdbcTemplate.update(
                "UPDATE reservations SET payment_receipt_file_id = ? WHERE id = ?",
                receiptId, reservationId);
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.applyAdminCarPauseCancellation(
                reservationId, true, now.plusHours(12));
        em.flush();

        // 3. Assert
        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(
                "cancelled_by_owner",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
        Assertions.assertEquals(
                Boolean.TRUE,
                jdbcTemplate.queryForObject(
                        "SELECT payment_refund_required FROM reservations WHERE id = ?",
                        Boolean.class,
                        reservationId));
    }

    @Test
    void testUpdateParticipantCancellationWithRefundMetaNoOpWhenPendingHasReceipt() {
        // 1. Arrange
        final long receiptId = insertStoredFile("race.pdf");
        final long reservationId = dao.createReservationForCar(
                riderId,
                carId,
                OffsetDateTime.parse("2026-06-15T10:00:00Z"),
                OffsetDateTime.parse("2026-06-20T18:00:00Z"),
                Reservation.Status.PENDING,
                new BigDecimal("100.00"),
                OffsetDateTime.parse("2026-06-14T10:00:00Z")).getId();
        final Reservation managed = em.find(Reservation.class, reservationId);
        managed.setPaymentReceiptFile(em.getReference(StoredFile.class, receiptId));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.updateParticipantCancellationWithRefundMeta(
                reservationId, "cancelled_by_rider", false, null);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(0, updated);
        Assertions.assertEquals(
                "pending",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
    }

    @Test
    void testFindReservationsWithDuePendingPaymentProofExcludesCancelledRows() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long pendingId = dao.createReservationForCar(
                riderId,
                carId,
                OffsetDateTime.parse("2026-06-15T10:00:00Z"),
                OffsetDateTime.parse("2026-06-20T18:00:00Z"),
                Reservation.Status.PENDING,
                new BigDecimal("100.00"),
                now.plusHours(1)).getId();
        insertReservation("cancelled_by_rider", false, null, null);
        em.flush();
        em.clear();

        // 2. Act
        final List<Reservation> due = dao.findReservationsWithDuePendingPaymentProof(now);

        // 3. Assert
        final List<Long> ids = due.stream().map(Reservation::getId).toList();
        Assertions.assertTrue(ids.contains(pendingId));
        Assertions.assertEquals(1, ids.size());
    }

    @Test
    void testCancelPendingMissingPaymentProofIfEligibleCancelsUnpaidPendingPastDeadline() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long reservationId = dao.createReservationForCar(
                riderId,
                carId,
                OffsetDateTime.parse("2026-06-15T10:00:00Z"),
                OffsetDateTime.parse("2026-06-20T18:00:00Z"),
                Reservation.Status.PENDING,
                new BigDecimal("100.00"),
                now.minusHours(1)).getId();
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.cancelPendingMissingPaymentProofIfEligible(reservationId, now);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(
                "cancelled_due_to_missing_payment_proof",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
    }

    @Test
    void testCancelPendingMissingPaymentProofIfEligibleNoOpWhenAlreadyAccepted() {
        // 1. Arrange — simulates sweep racing after receipt upload moved row to accepted.
        final OffsetDateTime now = OffsetDateTime.parse("2026-06-10T12:00:00Z");
        final long reservationId = createAcceptedReservation(
                OffsetDateTime.parse("2026-06-15T10:00:00Z"),
                OffsetDateTime.parse("2026-06-20T18:00:00Z"));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.cancelPendingMissingPaymentProofIfEligible(reservationId, now);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(0, updated);
        Assertions.assertEquals(
                "accepted",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
    }

    @Test
    void testHasActiveOverlapByCarReturnsTrueWhenBlockingReservationExists() {
        // 1. Arrange
        final OffsetDateTime start = OffsetDateTime.parse("2026-06-15T10:00:00Z");
        final OffsetDateTime end = OffsetDateTime.parse("2026-06-20T18:00:00Z");
        dao.createReservationForCar(
                riderId, carId, start, end, Reservation.Status.PENDING, new BigDecimal("100.00"),
                OffsetDateTime.parse("2026-06-14T10:00:00Z"));
        em.flush();
        em.clear();

        // 2. Act
        final boolean overlaps = dao.hasActiveOverlapByCar(carId, start, end);

        // 3. Assert
        Assertions.assertTrue(overlaps);
    }

    @Test
    void testAttachPaymentReceiptAndAcceptTransitionsPendingToAccepted() {
        // 1. Arrange
        final long receiptId = insertStoredFile("payment.pdf");
        final OffsetDateTime deadline = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24);
        final long reservationId = dao.createReservationForCar(
                riderId,
                carId,
                OffsetDateTime.parse("2099-06-15T10:00:00Z"),
                OffsetDateTime.parse("2099-06-20T18:00:00Z"),
                Reservation.Status.PENDING,
                new BigDecimal("100.00"),
                deadline).getId();
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.attachPaymentReceiptAndAccept(reservationId, riderId, receiptId);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(
                "accepted",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
        Assertions.assertEquals(
                receiptId,
                jdbcTemplate.queryForObject(
                        "SELECT payment_receipt_file_id FROM reservations WHERE id = ?",
                        Long.class,
                        reservationId));
    }

    @Test
    void testAttachPaymentReceiptAndAcceptNoOpWhenReceiptAlreadyAttached() {
        // 1. Arrange — race: sweep/upload already linked a receipt.
        final long existingReceiptId = insertStoredFile("existing.pdf");
        final long newReceiptId = insertStoredFile("duplicate.pdf");
        final OffsetDateTime deadline = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24);
        final long reservationId = dao.createReservationForCar(
                riderId,
                carId,
                OffsetDateTime.parse("2099-06-15T10:00:00Z"),
                OffsetDateTime.parse("2099-06-20T18:00:00Z"),
                Reservation.Status.PENDING,
                new BigDecimal("100.00"),
                deadline).getId();
        final Reservation managed = em.find(Reservation.class, reservationId);
        managed.setPaymentReceiptFile(em.getReference(StoredFile.class, existingReceiptId));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.attachPaymentReceiptAndAccept(reservationId, riderId, newReceiptId);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(0, updated);
        Assertions.assertEquals(
                existingReceiptId,
                jdbcTemplate.queryForObject(
                        "SELECT payment_receipt_file_id FROM reservations WHERE id = ?",
                        Long.class,
                        reservationId));
    }

    @Test
    void testCancelPendingMissingPaymentProofIfEligibleNoOpWhenPendingHasReceipt() {
        // 1. Arrange
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final long receiptId = insertStoredFile("paid-race.pdf");
        final long reservationId = dao.createReservationForCar(
                riderId,
                carId,
                OffsetDateTime.parse("2099-06-15T10:00:00Z"),
                OffsetDateTime.parse("2099-06-20T18:00:00Z"),
                Reservation.Status.PENDING,
                new BigDecimal("100.00"),
                now.minusHours(1)).getId();
        final Reservation managed = em.find(Reservation.class, reservationId);
        managed.setPaymentReceiptFile(em.getReference(StoredFile.class, receiptId));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.cancelPendingMissingPaymentProofIfEligible(reservationId, now);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(0, updated);
        Assertions.assertEquals(
                "pending",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM reservations WHERE id = ?", String.class, reservationId));
    }

    @Test
    void testAttachRefundReceiptLinksStoredFileWhenCancelledWithRefundRequired() {
        // 1. Arrange
        final long refundReceiptId = insertStoredFile("refund.pdf");
        final long reservationId = insertReservation(
                "cancelled_by_owner", true, null,
                OffsetDateTime.parse("2026-06-01T12:00:00Z"));
        em.flush();
        em.clear();

        // 2. Act
        final int updated = dao.attachRefundReceipt(reservationId, ownerId, refundReceiptId);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(1, updated);
        Assertions.assertEquals(
                refundReceiptId,
                jdbcTemplate.queryForObject(
                        "SELECT payment_refund_receipt_file_id FROM reservations WHERE id = ?",
                        Long.class,
                        reservationId));
        Assertions.assertEquals(
                Boolean.TRUE,
                jdbcTemplate.queryForObject(
                        "SELECT pending_refund_email_sent FROM reservations WHERE id = ?",
                        Boolean.class,
                        reservationId));
    }

    private long createAcceptedReservation(
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        return createAcceptedReservation(startDate, endDate, Reservation.Status.ACCEPTED);
    }

    private long createAcceptedReservation(
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status) {
        return dao.createReservationForCar(
                riderId, carId, startDate, endDate, status, new BigDecimal("100.00"), null).getId();
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
