package ar.edu.itba.paw.persistence.review;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class ReviewJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ReviewDao dao;

    @PersistenceContext
    private EntityManager em;

    private long ownerId;
    private long riderId;
    private long otherRiderId;
    private long modelId;

    @BeforeEach
    void seedFixture() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rv-owner@test.com", "Owner", "Test");
        ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "rv-owner@test.com");
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rv-rider@test.com", "Alice", "Rider");
        riderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "rv-rider@test.com");
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "rv-rider2@test.com", "Bob", "Other");
        otherRiderId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "rv-rider2@test.com");

        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Honda", true);
        final Long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Honda");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Civic", true, "SEDAN");
        modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Civic");
    }

    // ----- Fixture helpers (jdbc-only: never call into the DAO under test) -------------------

    private long insertCar(final String plate) {
        return insertCar(plate, ownerId);
    }

    private long insertCar(final String plate, final long carOwnerId) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        // The Car entity persists Powertrain/Transmission as STRING (uppercase). The JPA hydration
        // path used by findPublicReviewsForCar fails if the stored value does not match the enum name.
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, model_id, plate, powertrain, transmission, status, created_at, updated_at, minimum_rental_days) "
                        + "VALUES (?, ?, ?, 'GASOLINE', 'MANUAL', 'active', ?, ?, 1)",
                carOwnerId, modelId, plate, Timestamp.from(now.toInstant()), Timestamp.from(now.toInstant()));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, plate);
    }

    private long insertFinishedReservation(final long carId, final long aRiderId) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, car_id, start_date, end_date, status, total_price, created_at, updated_at, car_returned) "
                        + "VALUES (?, ?, ?, ?, 'finished', 100.00, ?, ?, TRUE)",
                aRiderId, carId,
                Timestamp.from(now.minusDays(5).toInstant()),
                Timestamp.from(now.minusDays(1).toInstant()),
                Timestamp.from(now.minusDays(6).toInstant()),
                Timestamp.from(now.toInstant()));
        // MAX(id) returns the just-inserted row deterministically; ORDER BY created_at can tie
        // when multiple inserts share the same timestamp, which produced flaky duplicates.
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM reservations", Long.class);
    }

    private void insertRiderReview(
            final long reservationId, final long carId, final Integer rating, final String comment,
            final OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO reviews (reservation_id, made_by_rider, car_id, created_at, rating, comment) "
                        + "VALUES (?, TRUE, ?, ?, ?, ?)",
                reservationId, carId, Timestamp.from(createdAt.toInstant()), rating, comment);
    }

    /** Owner-authored review of the rider ({@code made_by_rider = FALSE}). */
    private void insertOwnerReview(
            final long reservationId, final long carId, final Integer rating, final String comment,
            final OffsetDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO reviews (reservation_id, made_by_rider, car_id, created_at, rating, comment) "
                        + "VALUES (?, FALSE, ?, ?, ?, ?)",
                reservationId, carId, Timestamp.from(createdAt.toInstant()), rating, comment);
    }

    private long reviewIdFor(final long reservationId, final boolean madeByRider) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reviews WHERE reservation_id = ? AND made_by_rider = ?",
                Long.class, reservationId, madeByRider);
    }

    // ----- Tests -----------------------------------------------------------------------------

    @Test
    void testFindAverageRatingForCarExcludesOwnerToRiderReviews() {
        // 1. Arrange — rider rates car 5; owner rates rider 1. Car avg must stay 5.00.
        final long carId = insertCar("REV062");
        final long resRider = insertFinishedReservation(carId, riderId);
        final long resOwner = insertFinishedReservation(carId, otherRiderId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertRiderReview(resRider, carId, 5, "Great car", now.minusHours(1));
        insertOwnerReview(resOwner, carId, 1, "Bad rider", now);

        // 2. Act
        final BigDecimal avg = dao.findAverageRatingForCar(carId).orElseThrow();

        // 3. Assert
        Assertions.assertEquals(0, new BigDecimal("5.00").compareTo(avg));
    }

    @Test
    void testFindByIdReturnsHydratedReviewMatchingItsOwnSurrogateId() {
        // 1. Arrange
        final long carId = insertCar("REV100");
        final long reservationId = insertFinishedReservation(carId, riderId);
        insertRiderReview(reservationId, carId, 5, "Great trip", OffsetDateTime.now(ZoneOffset.UTC));
        final long reviewId = reviewIdFor(reservationId, true);

        // 2. Act
        final Optional<Review> found = dao.findById(reviewId);

        // 3. Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(reviewId, found.get().getId());
        Assertions.assertEquals(reservationId, found.get().getReservationId());
        Assertions.assertTrue(found.get().isMadeByRider());
        Assertions.assertEquals("Great trip", found.get().getComment().orElse(null));
    }

    @Test
    void testFindByIdReturnsEmptyForUnknownId() {
        // 2. Act
        final Optional<Review> found = dao.findById(Long.MAX_VALUE);

        // 3. Assert
        Assertions.assertTrue(found.isEmpty());
    }

    @Test
    void testInsertReviewPersistsRatingCommentAndMadeByRider() {
        // 1.Arrange
        final long carId = insertCar("REV120");
        final long reservationId = insertFinishedReservation(carId, riderId);

        // 2.Act
        dao.insertReview(reservationId, true, 5, "Excellent trip", null);
        em.flush();

        // 3.Assert
        Assertions.assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
                "SELECT made_by_rider FROM reviews WHERE reservation_id = ?", Boolean.class, reservationId));
        Assertions.assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT rating FROM reviews WHERE reservation_id = ?", Integer.class, reservationId));
        Assertions.assertEquals("Excellent trip", jdbcTemplate.queryForObject(
                "SELECT comment FROM reviews WHERE reservation_id = ?", String.class, reservationId));
        Assertions.assertEquals(carId, jdbcTemplate.queryForObject(
                "SELECT car_id FROM reviews WHERE reservation_id = ?", Long.class, reservationId));
    }

    @Test
    void testExistsReviewReturnsTrueForSeededRiderReview() {
        // 1.Arrange — JDBC seed; Act only exercises the read under test.
        final long carId = insertCar("REV121");
        final long reservationId = insertFinishedReservation(carId, riderId);
        insertRiderReview(reservationId, carId, 5, "Seeded", OffsetDateTime.now(ZoneOffset.UTC));

        // 2.Act / 3.Assert
        Assertions.assertTrue(dao.existsReview(reservationId, true));
        Assertions.assertFalse(dao.existsReview(reservationId, false));
    }

    @Test
    void testReviewsOnTheSameReservationGetDistinctSurrogateIds() {
        // 1. Arrange — owner and rider both review the same finished reservation.
        final long carId = insertCar("REV110");
        final long reservationId = insertFinishedReservation(carId, riderId);
        insertRiderReview(reservationId, carId, 4, "Rider side", OffsetDateTime.now(ZoneOffset.UTC));
        insertOwnerReview(reservationId, carId, 5, "Owner side", OffsetDateTime.now(ZoneOffset.UTC));

        // 2. Act
        final long riderReviewId = reviewIdFor(reservationId, true);
        final long ownerReviewId = reviewIdFor(reservationId, false);

        // 3. Assert — each side of the same reservation has its own unique URN-backing id.
        Assertions.assertNotEquals(riderReviewId, ownerReviewId,
                "Reviews on the same reservation must not share a surrogate id");
    }
}
