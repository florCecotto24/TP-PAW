package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;
import ar.edu.itba.paw.persistence.ReservationAvailabilityDao;

/**
 * Phase 7e coverage: pure N:N bridge. Per-day price is reconstructed by filtering candidates by date
 * range and picking the latest {@code createdAt}; no segmentation is persisted.
 */
class ReservationAvailabilityJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ReservationAvailabilityDao dao;

    @PersistenceContext
    private EntityManager em;

    private long carId;
    private long riderId;
    private long availabilityId100;
    private long availabilityId200;

    @BeforeEach
    void seedCarAndAvailabilities() {
        jdbcTemplate.update("DELETE FROM reservations_availabilities");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM car_availability");
        jdbcTemplate.update("DELETE FROM cars");
        jdbcTemplate.update("DELETE FROM users WHERE email IN (?, ?)",
                "ra-bridge@test.com", "rider-bridge@test.com");

        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "ra-bridge@test.com", "Owner", "Test");
        final long ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "ra-bridge@test.com");
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rider-bridge@test.com", "Rider", "Test");
        this.riderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "rider-bridge@test.com");
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, transmission, powertrain) VALUES (?, ?, ?, ?)",
                ownerId, "RAB01", "MANUAL", "GASOLINE");
        this.carId = jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, "RAB01");

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        availabilityId100 = insertAvailability(carId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                new BigDecimal("100.00"), now);
        availabilityId200 = insertAvailability(carId, LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 20),
                new BigDecimal("200.00"), now.plusMinutes(1));
    }

    @Test
    void testSumReservationTotalReconstructsPerDayFromBridgedAvailabilities() {
        // Reservation covers 6 wall-calendar days: Jun 8..10 (3 days @100) + Jun 11..13 (3 days @200) = 900
        final long reservationId = insertReservation("2026-06-08T10:00", "2026-06-13T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100, availabilityId200));

        final BigDecimal total = dao.sumReservationTotal(reservationId).orElseThrow();

        Assertions.assertEquals(0, new BigDecimal("900.00").compareTo(total));
    }

    @Test
    void testSumReservationTotalPicksMostRecentlyCreatedAvailabilityWhenRangesOverlap() {
        // Newer availability fully contains the older one for days 5..8 with day_price 300.
        final OffsetDateTime newerCreated = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        final long overrideAvailabilityId = insertAvailability(
                carId, LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 8), new BigDecimal("300.00"), newerCreated);

        // Reservation Jun 5..8 (4 days). Both candidates cover all days; the newer wins -> 4 * 300 = 1200.
        final long reservationId = insertReservation("2026-06-05T10:00", "2026-06-08T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100, overrideAvailabilityId));

        final BigDecimal total = dao.sumReservationTotal(reservationId).orElseThrow();

        Assertions.assertEquals(0, new BigDecimal("1200.00").compareTo(total));
    }

    @Test
    void testSumReservationTotalIsEmptyWhenAnyDayLacksCoverage() {
        // Reservation Jun 8..15 but we only bridge availabilityId100 which covers Jun 1..10.
        // Jun 11..15 lack coverage -> empty.
        final long reservationId = insertReservation("2026-06-08T10:00", "2026-06-15T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100));

        Assertions.assertTrue(dao.sumReservationTotal(reservationId).isEmpty());
    }

    @Test
    void testFindEffectivePickupAvailabilityForReservationPicksBridgedCandidateForFirstDay() {
        // 1. Arrange — reservation falls inside availabilityId100 (Jun 1..10 @ 100). Bridge both.
        final long reservationId = insertReservation("2026-06-03T10:00", "2026-06-05T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100, availabilityId200));

        // 2. Act
        final var winner = dao.findEffectivePickupAvailabilityForReservation(reservationId);

        // 3. Assert — only availabilityId100 covers Jun 3 (the first day); availabilityId200 starts Jun 11.
        Assertions.assertTrue(winner.isPresent());
        Assertions.assertEquals(availabilityId100, winner.get().getId());
    }

    @Test
    void testFindEffectivePickupAvailabilityForReservationPicksLatestCreatedAmongBridgedCovers() {
        // 1. Arrange — bridge an OFFERED row that covers the first day and was created LATER than
        // availabilityId100. The newer one should win even though both cover the first day.
        final OffsetDateTime newerCreated = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        final long newerId = insertAvailability(
                carId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("350.00"), newerCreated);
        final long reservationId = insertReservation("2026-06-03T10:00", "2026-06-05T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100, newerId));

        // 2. Act
        final var winner = dao.findEffectivePickupAvailabilityForReservation(reservationId);

        // 3. Assert
        Assertions.assertTrue(winner.isPresent());
        Assertions.assertEquals(newerId, winner.get().getId());
    }

    @Test
    void testFindEffectivePickupAvailabilityForReservationIgnoresUnbridgedCarAvailabilities() {
        // 1. Arrange — bridge ONLY availabilityId100. Insert a NEWER row on the car that ALSO covers
        // the first day; it is NOT bridged. The bridge-anchored resolver must ignore it. This is the
        // exact scenario where an owner edit inserts a new OFFERED row on top of an existing
        // reservation's range: the rider's pickup snapshot must stay stable.
        final OffsetDateTime newerCreated = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(20);
        insertAvailability(
                carId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("9999.99"), newerCreated);
        final long reservationId = insertReservation("2026-06-03T10:00", "2026-06-05T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100));

        // 2. Act
        final var winner = dao.findEffectivePickupAvailabilityForReservation(reservationId);

        // 3. Assert — the bridged availabilityId100 wins; the newer unbridged row is invisible.
        Assertions.assertTrue(winner.isPresent());
        Assertions.assertEquals(availabilityId100, winner.get().getId());
    }

    @Test
    void testFindEffectivePickupAvailabilityForReservationSkipsWithdrawnBridgedCandidates() {
        // 1. Arrange — bridge a WITHDRAWN row covering the first day plus an OFFERED row covering it too;
        // the OFFERED one must win (sum semantics).
        final OffsetDateTime newerCreated = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
        final long withdrawnId = insertAvailabilityWithKind(
                carId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("100.00"), newerCreated, "withdrawn");
        final long reservationId = insertReservation("2026-06-03T10:00", "2026-06-05T18:00");
        dao.insertCoveringAvailabilities(reservationId, List.of(availabilityId100, withdrawnId));

        // 2. Act
        final var winner = dao.findEffectivePickupAvailabilityForReservation(reservationId);

        // 3. Assert — withdrawnId is more recent but is filtered out; availabilityId100 wins.
        Assertions.assertTrue(winner.isPresent());
        Assertions.assertEquals(availabilityId100, winner.get().getId());
    }

    @Test
    void testFindEffectivePickupAvailabilityForReservationIsEmptyWhenReservationHasNoBridgeRows() {
        // 1. Arrange — reservation without bridge entries (e.g. legacy data).
        final long reservationId = insertReservation("2026-06-03T10:00", "2026-06-05T18:00");

        // 2. Act
        final var winner = dao.findEffectivePickupAvailabilityForReservation(reservationId);

        // 3. Assert
        Assertions.assertTrue(winner.isEmpty());
    }


    private long insertReservation(final String startUtc, final String endUtc) {
        final OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, car_id, start_date, end_date, status, total_price, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                riderId, carId,
                OffsetDateTime.parse(startUtc + ":00Z"),
                OffsetDateTime.parse(endUtc + ":00Z"),
                "pending", BigDecimal.ZERO, created, created);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservations WHERE rider_id = ? AND start_date = ?",
                Long.class, riderId, OffsetDateTime.parse(startUtc + ":00Z"));
    }

    private long insertAvailability(
            final long carId,
            final LocalDate start,
            final LocalDate end,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt) {
        return insertAvailabilityWithKind(carId, start, end, dayPrice, createdAt, "offered");
    }

    private long insertAvailabilityWithKind(
            final long carId,
            final LocalDate start,
            final LocalDate end,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt,
            final String kind) {
        jdbcTemplate.update(
                "INSERT INTO car_availability (car_id, start_date, end_date, created_at, updated_at, "
                        + "day_price, start_point_street, check_in_time, check_out_time, kind) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                carId, start, end, createdAt, createdAt, dayPrice,
                "Street", LocalTime.of(10, 0), LocalTime.of(18, 0), kind);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM car_availability WHERE car_id = ? AND start_date = ? AND day_price = ? AND kind = ? "
                        + "ORDER BY id DESC LIMIT 1",
                Long.class,
                carId,
                start,
                dayPrice,
                kind);
    }
}
