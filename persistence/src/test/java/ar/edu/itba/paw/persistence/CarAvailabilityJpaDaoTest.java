package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;

/** Phase 7 coverage: layered availability rows with kind and "most recent createdAt wins" lookup. */
class ListingAvailabilityJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ListingAvailabilityDao dao;

    @PersistenceContext
    private EntityManager em;

    private long carId;

    @BeforeEach
    void seedCar() {
        jdbcTemplate.update("DELETE FROM listing_availability");
        jdbcTemplate.update("DELETE FROM cars");
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", "la-owner@test.com");

        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "la-owner@test.com", "Owner", "Test");
        final long ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "la-owner@test.com");
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, transmission, powertrain) VALUES (?, ?, ?, ?)",
                ownerId, "AVL01", "MANUAL", "GASOLINE");
        this.carId = jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, "AVL01");
    }

    private long insertAvailability(
            final LocalDate start, final LocalDate end, final BigDecimal dayPrice,
            final String kind, final OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO listing_availability (car_id, start_date, end_date, day_price, "
                        + "start_point_street, check_in_time, check_out_time, kind, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                carId, start, end, dayPrice,
                "Belgrano", LocalTime.of(10, 0), LocalTime.of(18, 0), kind, createdAt, createdAt);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM listing_availability WHERE car_id = ? AND start_date = ? AND end_date = ? AND created_at = ?",
                Long.class, carId, start, end, createdAt);
    }

    @Test
    void testFindEffectiveForDayPicksMostRecentCreatedAtWhenTwoRowsOverlap() {
        // 1. Arrange — two overlapping offers, second one is more recent.
        final OffsetDateTime t1 = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final OffsetDateTime t2 = OffsetDateTime.parse("2026-07-02T10:00:00Z");
        final long oldId = insertAvailability(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10),
                new BigDecimal("100.00"), "offered", t1);
        final long newId = insertAvailability(
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 9),
                new BigDecimal("150.00"), "offered", t2);

        // 2. Act
        final Optional<ListingAvailability> winnerDay5 = dao.findEffectiveForDayByCar(carId, LocalDate.of(2026, 8, 5));
        final Optional<ListingAvailability> winnerDay1 = dao.findEffectiveForDayByCar(carId, LocalDate.of(2026, 8, 1));

        // 3. Assert — day 5 only the more recent covers it (and even if both did, more recent wins).
        Assertions.assertTrue(winnerDay5.isPresent());
        Assertions.assertEquals(newId, winnerDay5.get().getId());
        Assertions.assertEquals(0, new BigDecimal("150.00").compareTo(winnerDay5.get().getDayPriceValue()));
        Assertions.assertTrue(winnerDay1.isPresent());
        Assertions.assertEquals(oldId, winnerDay1.get().getId());
    }

    @Test
    void testFindEffectiveForDayReturnsWithdrawnRowWhenItIsTheMostRecent() {
        // 1. Arrange — offered (older) then withdrawn (newer) for the same day.
        final OffsetDateTime t1 = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final OffsetDateTime t2 = OffsetDateTime.parse("2026-07-05T10:00:00Z");
        insertAvailability(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                new BigDecimal("100.00"), "offered", t1);
        final long withdrawnId = insertAvailability(
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 3),
                new BigDecimal("100.00"), "withdrawn", t2);

        // 2. Act
        final Optional<ListingAvailability> winner = dao.findEffectiveForDayByCar(carId, LocalDate.of(2026, 9, 3));

        // 3. Assert
        Assertions.assertTrue(winner.isPresent());
        Assertions.assertEquals(withdrawnId, winner.get().getId());
        Assertions.assertEquals(ListingAvailability.Kind.WITHDRAWN, winner.get().getKind());
    }

    @Test
    void testFindEffectiveForDayReturnsEmptyWhenNoRowCoversTheDay() {
        // 1. Arrange
        final OffsetDateTime t = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        insertAvailability(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5),
                new BigDecimal("100.00"), "offered", t);

        // 2. Act
        final Optional<ListingAvailability> winner = dao.findEffectiveForDayByCar(carId, LocalDate.of(2026, 10, 6));

        // 3. Assert
        Assertions.assertTrue(winner.isEmpty());
    }

    @Test
    void testFindByIdReturnsRowWhenPresent() {
        // 1. Arrange
        final OffsetDateTime t = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final long id = insertAvailability(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                new BigDecimal("100.00"), "offered", t);

        // 2. Act
        final Optional<ListingAvailability> result = dao.findById(id);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(id, result.get().getId());
    }

    @Test
    void testFindByIdReturnsEmptyWhenAbsent() {
        // 1. Arrange — no rows.

        // 2. Act
        final Optional<ListingAvailability> result = dao.findById(999_999L);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void testFindOverlappingRangeReturnsAllRowsTouchingTheWindowOrderedByCreatedAtDesc() {
        // 1. Arrange — three rows: one before, one inside, one withdrawn inside.
        final OffsetDateTime t1 = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final OffsetDateTime t2 = OffsetDateTime.parse("2026-07-02T10:00:00Z");
        final OffsetDateTime t3 = OffsetDateTime.parse("2026-07-03T10:00:00Z");
        insertAvailability(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                new BigDecimal("90.00"), "offered", t1);
        insertAvailability(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("110.00"), "offered", t2);
        insertAvailability(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 12),
                new BigDecimal("110.00"), "withdrawn", t3);

        // 2. Act
        final List<ListingAvailability> result =
                dao.findOverlappingRangeByCar(carId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        // 3. Assert — only the two August rows match, newest first.
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(ListingAvailability.Kind.WITHDRAWN, result.get(0).getKind());
        Assertions.assertEquals(ListingAvailability.Kind.OFFERED, result.get(1).getKind());
    }

    @Test
    void testCreateFullForCarPersistsCarId() {
        // 1. Arrange — no extra setup beyond @BeforeEach.

        // 2. Act
        final ListingAvailability created = dao.createFullForCar(
                carId,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 5),
                new BigDecimal("120.00"),
                "Belgrano",
                null,
                null,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                ListingAvailability.Kind.OFFERED);
        em.flush();

        // 3. Assert
        final Long persistedCarId = jdbcTemplate.queryForObject(
                "SELECT car_id FROM listing_availability WHERE id = ?", Long.class, created.getId());
        Assertions.assertEquals(carId, persistedCarId.longValue());
        Assertions.assertEquals(carId, created.getCarId());
    }
}
