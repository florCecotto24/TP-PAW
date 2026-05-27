package ar.edu.itba.paw.persistence.hibernate;

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

import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
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
        jdbcTemplate.update("DELETE FROM listing_availability");
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
        jdbcTemplate.update(
                "INSERT INTO listing_availability (car_id, start_date, end_date, created_at, updated_at, "
                        + "day_price, start_point_street, check_in_time, check_out_time, kind) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                carId, start, end, createdAt, createdAt, dayPrice,
                "Street", LocalTime.of(10, 0), LocalTime.of(18, 0), "offered");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM listing_availability WHERE car_id = ? AND start_date = ? AND day_price = ?",
                Long.class,
                carId,
                start,
                dayPrice);
    }
}
