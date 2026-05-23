package ar.edu.itba.paw.persistence.hibernate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.ReservationAvailabilityLink;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import ar.edu.itba.paw.persistence.ReservationAvailabilityDao;

/** Phase 3 coverage: bridge-table pricing via SQL day_price * covered days. */
class ReservationAvailabilityJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ReservationAvailabilityDao dao;

    private long carId;
    private long availabilityId100;
    private long availabilityId200;

    @BeforeEach
    void seedListingAndAvailabilities() {
        jdbcTemplate.update("DELETE FROM reservations_availabilities");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM listing_availability");
        jdbcTemplate.update("DELETE FROM listings");
        jdbcTemplate.update("DELETE FROM cars");
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", "ra-bridge@test.com");

        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "ra-bridge@test.com", "Owner", "Test");
        final long ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "ra-bridge@test.com");
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, brand, model, type, transmission, powertrain) VALUES (?, ?, ?, ?, ?, ?, ?)",
                ownerId, "RAB01", "Brand", "Model", "SEDAN", "MANUAL", "GASOLINE");
        final long carId = jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, "RAB01");
        this.carId = carId;

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO listings (title, car_id, created_at, updated_at, status, day_price, "
                        + "start_point_street, description, check_in_time, check_out_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "Bridge listing", carId, now, now, "active", new BigDecimal("999.00"),
                "Street", "Description", LocalTime.of(10, 0), LocalTime.of(18, 0));
        final long listingId = jdbcTemplate.queryForObject(
                "SELECT id FROM listings WHERE car_id = ?", Long.class, carId);

        availabilityId100 = insertAvailability(listingId, carId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                new BigDecimal("100.00"), now);
        availabilityId200 = insertAvailability(listingId, carId, LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 20),
                new BigDecimal("200.00"), now.plusMinutes(1));
    }

    @Test
    void testQuoteTotalFromLinksWhenSingleChunkReturnsDayPriceTimesCoveredDays() {
        final List<ReservationAvailabilityLink> links = List.of(
                new ReservationAvailabilityLink(
                        availabilityId100,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 5)));

        final BigDecimal total = dao.quoteTotalFromLinks(links).orElseThrow();

        Assertions.assertEquals(0, new BigDecimal("500.00").compareTo(total));
    }

    @Test
    void testQuoteTotalFromLinksWhenTwoChunksReturnsSumInSql() {
        final List<ReservationAvailabilityLink> links = List.of(
                new ReservationAvailabilityLink(
                        availabilityId100,
                        LocalDate.of(2026, 6, 8),
                        LocalDate.of(2026, 6, 10)),
                new ReservationAvailabilityLink(
                        availabilityId200,
                        LocalDate.of(2026, 6, 11),
                        LocalDate.of(2026, 6, 12)));

        final BigDecimal total = dao.quoteTotalFromLinks(links).orElseThrow();

        // 3 * 100 + 2 * 200 = 700
        Assertions.assertEquals(0, new BigDecimal("700.00").compareTo(total));
    }

    @Test
    void testSumReservationTotalWhenBridgeRowsExistMatchesQuote() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rider-bridge@test.com", "Rider", "Test");
        final long riderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "rider-bridge@test.com");
        final long listingId = jdbcTemplate.queryForObject(
                "SELECT listing_id FROM listing_availability WHERE id = ?", Long.class, availabilityId100);

        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, listing_id, car_id, start_date, end_date, status, total_price, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                riderId, listingId, carId, now, now.plusDays(5), "pending", BigDecimal.ZERO, now, now);
        final long reservationId = jdbcTemplate.queryForObject(
                "SELECT id FROM reservations WHERE rider_id = ?", Long.class, riderId);

        final List<ReservationAvailabilityLink> links = List.of(
                new ReservationAvailabilityLink(
                        availabilityId100,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 3)),
                new ReservationAvailabilityLink(
                        availabilityId200,
                        LocalDate.of(2026, 6, 11),
                        LocalDate.of(2026, 6, 12)));
        dao.insertLinks(reservationId, links);

        final BigDecimal quoted = dao.quoteTotalFromLinks(links).orElseThrow();
        final BigDecimal stored = dao.sumReservationTotal(reservationId).orElseThrow();

        Assertions.assertEquals(0, quoted.compareTo(stored));
        Assertions.assertEquals(0, new BigDecimal("700.00").compareTo(stored));
    }

    private long insertAvailability(
            final long listingId,
            final long carId,
            final LocalDate start,
            final LocalDate end,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO listing_availability (listing_id, car_id, start_date, end_date, created_at, updated_at, "
                        + "day_price, start_point_street, check_in_time, check_out_time, kind) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                listingId, carId, start, end, createdAt, createdAt, dayPrice,
                "Street", LocalTime.of(10, 0), LocalTime.of(18, 0), "offered");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM listing_availability WHERE listing_id = ? AND start_date = ?",
                Long.class,
                listingId,
                start);
    }
}
