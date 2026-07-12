package ar.edu.itba.paw.persistence.review;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.review.Review;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarPublicReview;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
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
        // path used by findCarPublicReviews fails if the stored value does not match the enum name.
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

    private long insertImage() {
        jdbcTemplate.update(
                "INSERT INTO images (image_name, content_type, byte_array) VALUES (?, ?, ?)",
                "review.png", "image/png", new byte[] {1, 2, 3});
        return jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM images", Long.class);
    }

    private void linkReviewImage(final long reservationId, final long imageId) {
        jdbcTemplate.update(
                "UPDATE reviews SET image_id = ? WHERE reservation_id = ? AND made_by_rider = TRUE",
                imageId, reservationId);
    }

    private long reviewIdFor(final long reservationId, final boolean madeByRider) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reviews WHERE reservation_id = ? AND made_by_rider = ?",
                Long.class, reservationId, madeByRider);
    }

    // ----- Tests -----------------------------------------------------------------------------

    @Test
    void testFindCarPublicReviewsReturnsMostRecentFirstAndExcludesOmittedReviews() {
        // 1. Arrange — three rider reviews on the same car, one with rating=null (omitted).
        final long carId = insertCar("REV001");
        final long resOld = insertFinishedReservation(carId, riderId);
        final long resNew = insertFinishedReservation(carId, otherRiderId);
        final long resOmitted = insertFinishedReservation(carId, riderId);
        final OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);
        insertRiderReview(resOld, carId, 4, "Solid car", base);
        insertRiderReview(resNew, carId, 5, "Loved it", base.plusHours(2));
        insertRiderReview(resOmitted, carId, null, null, base.plusHours(4));

        // 2. Act
        final Page<CarPublicReview> page = dao.findCarPublicReviews(carId, 0, 10);

        // 3. Assert
        Assertions.assertEquals(2, page.getContent().size(), "Reviews with rating=null must be excluded");
        Assertions.assertEquals(2L, page.getTotalItems());
        Assertions.assertEquals(5, page.getContent().get(0).getRating(),
                "Newest review (rating 5) must come first");
        Assertions.assertEquals(4, page.getContent().get(1).getRating());
        Assertions.assertEquals("Bob", page.getContent().get(0).getReviewerForename(),
                "Newest review must be attributed to the right reviewer");
    }

    @Test
    void testFindCarPublicReviewsExcludesReviewsOfOtherCars() {
        // 1. Arrange
        final long carA = insertCar("REV010");
        final long carB = insertCar("REV011");
        final long resOnA = insertFinishedReservation(carA, riderId);
        final long resOnB = insertFinishedReservation(carB, riderId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertRiderReview(resOnA, carA, 5, "On A", now.minusHours(1));
        insertRiderReview(resOnB, carB, 3, "On B", now);

        // 2. Act
        final Page<CarPublicReview> page = dao.findCarPublicReviews(carA, 0, 10);

        // 3. Assert
        Assertions.assertEquals(1L, page.getTotalItems());
        Assertions.assertEquals("On A", page.getContent().get(0).getComment().orElse(null));
    }

    @Test
    void testFindCarPublicReviewsPaginatesAtSqlLevel() {
        // 1. Arrange — three reviews on the same car, paged 2-per-page.
        final long carId = insertCar("REV020");
        final long resA = insertFinishedReservation(carId, riderId);
        final long resB = insertFinishedReservation(carId, otherRiderId);
        final long resC = insertFinishedReservation(carId, riderId);
        final OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        insertRiderReview(resA, carId, 3, "A", base);
        insertRiderReview(resB, carId, 4, "B", base.plusHours(1));
        insertRiderReview(resC, carId, 5, "C", base.plusHours(2));

        // 2. Act
        final Page<CarPublicReview> firstPage = dao.findCarPublicReviews(carId, 0, 2);
        final Page<CarPublicReview> secondPage = dao.findCarPublicReviews(carId, 1, 2);

        // 3. Assert — newest-first ordering means firstPage = [C, B], secondPage = [A].
        Assertions.assertEquals(3L, firstPage.getTotalItems());
        Assertions.assertEquals(List.of("C", "B"),
                firstPage.getContent().stream().map(r -> r.getComment().orElse("")).toList());
        Assertions.assertEquals(1, secondPage.getContent().size());
        Assertions.assertEquals("A", secondPage.getContent().get(0).getComment().orElse(null));
    }

    @Test
    void testFindCarPublicReviewsHydratesImageIdWhenReviewHasAttachedImage() {
        // 1. Arrange
        final long carId = insertCar("REV030");
        final long reservationId = insertFinishedReservation(carId, riderId);
        insertRiderReview(reservationId, carId, 5, "With photo", OffsetDateTime.now(ZoneOffset.UTC));
        final long imageId = insertImage();
        linkReviewImage(reservationId, imageId);

        // 2. Act
        final Page<CarPublicReview> page = dao.findCarPublicReviews(carId, 0, 10);

        // 3. Assert
        Assertions.assertEquals(1L, page.getTotalItems());
        Assertions.assertEquals(Long.valueOf(imageId),
                page.getContent().get(0).getImageId().orElse(null));
    }

    @Test
    void testFindCarPublicReviewsReturnsEmptyPageWhenCarHasNoReviews() {
        // 1. Arrange
        final long carId = insertCar("REV040");

        // 2. Act
        final Page<CarPublicReview> page = dao.findCarPublicReviews(carId, 0, 10);

        // 3. Assert
        Assertions.assertTrue(page.getContent().isEmpty());
        Assertions.assertEquals(0L, page.getTotalItems());
    }

    @Test
    void testCountReviewsForCarCountsOnlyRatedReviewsOfThatCar() {
        // 1. Arrange
        final long carA = insertCar("REV050");
        final long carB = insertCar("REV051");
        final long resA1 = insertFinishedReservation(carA, riderId);
        final long resA2 = insertFinishedReservation(carA, otherRiderId);
        final long resAOmitted = insertFinishedReservation(carA, riderId);
        final long resB1 = insertFinishedReservation(carB, riderId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertRiderReview(resA1, carA, 4, "first", now.minusHours(3));
        insertRiderReview(resA2, carA, 5, "second", now.minusHours(2));
        insertRiderReview(resAOmitted, carA, null, null, now.minusHours(1));
        insertRiderReview(resB1, carB, 3, "elsewhere", now);

        // 2. Act
        final long totalForA = dao.countReviewsForCar(carA);
        final long totalForB = dao.countReviewsForCar(carB);

        // 3. Assert
        Assertions.assertEquals(2L, totalForA, "Only the two rated reviews of car A must count");
        Assertions.assertEquals(1L, totalForB);
    }

    @Test
    void testCountReviewsForCarReturnsZeroForCarWithoutReviews() {
        // 1. Arrange
        final long carId = insertCar("REV060");

        // 2. Act
        final long total = dao.countReviewsForCar(carId);

        // 3. Assert
        Assertions.assertEquals(0L, total);
    }

    @Test
    void testFindReviewsForUserPageMergesOwnerAndRiderDirectionsOrderedByDateDesc() {
        // 1. Arrange — ownerId receives a rider review as owner of carA, and an owner review as
        // rider of carB (owned by otherRiderId). The two rows come from opposite `made_by_rider`
        // branches and must be merged into a single date-ordered feed.
        final long carA = insertCar("REV070", ownerId);
        final long carB = insertCar("REV071", otherRiderId);
        final long resOnA = insertFinishedReservation(carA, riderId);
        final long resOnB = insertFinishedReservation(carB, ownerId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertRiderReview(resOnA, carA, 4, "Rider praises owner's car", now.minusHours(2));
        insertOwnerReview(resOnB, carB, 5, "Owner praises this rider", now.minusHours(1));

        // 2. Act
        final Page<ReviewItemDto> page = dao.findReviewsForUserPage(ownerId, 0, 10);

        // 3. Assert — most recent (owner review) first.
        Assertions.assertEquals(2L, page.getTotalItems());
        Assertions.assertEquals(
                List.of("Owner praises this rider", "Rider praises owner's car"),
                page.getContent().stream().map(ReviewItemDto::getCommentText).toList());
    }

    @Test
    void testFindReviewsForUserPagePaginatesAtSqlLevel() {
        // 1. Arrange — three rider reviews of the same owner, paged 2-per-page.
        final long carId = insertCar("REV080", ownerId);
        final long resA = insertFinishedReservation(carId, riderId);
        final long resB = insertFinishedReservation(carId, otherRiderId);
        final long resC = insertFinishedReservation(carId, riderId);
        final OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2);
        insertRiderReview(resA, carId, 3, "A", base);
        insertRiderReview(resB, carId, 4, "B", base.plusHours(1));
        insertRiderReview(resC, carId, 5, "C", base.plusHours(2));

        // 2. Act
        final Page<ReviewItemDto> firstPage = dao.findReviewsForUserPage(ownerId, 0, 2);
        final Page<ReviewItemDto> secondPage = dao.findReviewsForUserPage(ownerId, 1, 2);

        // 3. Assert — newest-first ordering means firstPage = [C, B], secondPage = [A].
        Assertions.assertEquals(3L, firstPage.getTotalItems());
        Assertions.assertEquals(List.of("C", "B"),
                firstPage.getContent().stream().map(ReviewItemDto::getCommentText).toList());
        Assertions.assertEquals(1, secondPage.getContent().size());
        Assertions.assertEquals("A", secondPage.getContent().get(0).getCommentText());
    }

    @Test
    void testFindReviewsForUserPageExcludesOmittedReviewsAndOtherUsers() {
        // 1. Arrange
        final long carId = insertCar("REV090", ownerId);
        final long resRated = insertFinishedReservation(carId, riderId);
        final long resOmitted = insertFinishedReservation(carId, riderId);
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertRiderReview(resRated, carId, 5, "Great", now.minusHours(1));
        insertRiderReview(resOmitted, carId, null, null, now);

        // 2. Act
        final Page<ReviewItemDto> ownerPage = dao.findReviewsForUserPage(ownerId, 0, 10);
        final Page<ReviewItemDto> unrelatedUserPage = dao.findReviewsForUserPage(otherRiderId, 0, 10);

        // 3. Assert
        Assertions.assertEquals(1L, ownerPage.getTotalItems(), "Omitted (rating=null) review must be excluded");
        Assertions.assertEquals(0L, unrelatedUserPage.getTotalItems());
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
        Assertions.assertTrue(dao.existsReview(reservationId, true));
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
