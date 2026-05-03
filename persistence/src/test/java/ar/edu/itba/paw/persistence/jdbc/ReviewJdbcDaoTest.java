package ar.edu.itba.paw.persistence.jdbc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.dto.ListingPublicReview;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.profile.ReviewItemDto;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;

class ReviewJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ReviewJdbcDao dao;

    private static final long OWNER_ID = 1L;
    private static final long RIDER_ID = 2L;
    private static final long CAR_ID = 10L;
    private static final long LISTING_ID = 100L;
    private static final long RESERVATION_ID = 1000L;

    @BeforeEach
    void setUpFixtures() {

        insertUser(OWNER_ID, "owner@mail.com", "Owen", "Owner");
        insertUser(RIDER_ID, "rider@mail.com", "Rider", "Two");
        insertCar(CAR_ID, OWNER_ID, "P1", "Ford", "Fiesta",
                Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final OffsetDateTime base = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertListing(LISTING_ID, CAR_ID, "L", Listing.Status.ACTIVE, new BigDecimal("30.00"), base);
        insertReservation(RESERVATION_ID, RIDER_ID, LISTING_ID,
                base.plusDays(10), base.plusDays(15),
                Reservation.Status.FINISHED, base, base.plusDays(15));
    }

    private void insertReview(final boolean madeByRider, final OffsetDateTime createdAt,
                              final int rating, final String comment) {
        jdbcTemplate.update(
                "INSERT INTO reviews(reservation_id, made_by_rider, created_at, rating, comment) VALUES (?, ?, ?, ?, ?)",
                RESERVATION_ID, madeByRider, Timestamp.from(createdAt.toInstant()), rating, comment);
    }

    @Test
    void testExistsReviewReturnsTrueOnlyForMatchingMadeByRiderFlag() {
        // 1.Arrange
        insertReview(true, OffsetDateTime.parse("2026-05-01T12:00:00Z"), 5, "Great owner");

        // 2.Exercise
        final boolean asRider = dao.existsReview(RESERVATION_ID, true);
        final boolean asOwner = dao.existsReview(RESERVATION_ID, false);

        // 3.Assert
        Assertions.assertTrue(asRider);
        Assertions.assertFalse(asOwner);
    }

    @Test
    void testInsertReviewPersistsRowVisibleViaJdbcTemplate() {

        // 2.Exercise
        dao.insertReview(RESERVATION_ID, true, 4, "Solid car");

        // 3.Assert
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT reservation_id, made_by_rider, rating, comment FROM reviews "
                        + "WHERE reservation_id = ? AND made_by_rider = ?",
                RESERVATION_ID, true);
        Assertions.assertEquals(RESERVATION_ID, ((Number) row.get("RESERVATION_ID")).longValue());
        Assertions.assertEquals(Boolean.TRUE, row.get("MADE_BY_RIDER"));
        Assertions.assertEquals(4, ((Number) row.get("RATING")).intValue());
        Assertions.assertEquals("Solid car", row.get("COMMENT"));
    }

    @Test
    void testCountReviewsForListingReturnsTotalRowsForListing() {
        // 1.Arrange
        insertReview(true, OffsetDateTime.parse("2026-05-01T12:00:00Z"), 5, "rider review");
        insertReview(false, OffsetDateTime.parse("2026-05-02T12:00:00Z"), 4, "owner review");

        // 2.Exercise
        final long count = dao.countReviewsForListing(LISTING_ID);

        // 3.Assert
        Assertions.assertEquals(2L, count);
    }

    @Test
    void testCountReviewsForListingReturnsZeroWhenListingHasNoReviews() {
        // 1.Arrange / 2.Exercise
        final long count = dao.countReviewsForListing(LISTING_ID);

        // 3.Assert
        Assertions.assertEquals(0L, count);
    }

    @Test
    void testFindListingPublicReviewsReturnsRowsOrderedByCreatedAtDescWithRiderReviewerName() {
        // 1.Arrange
        insertReview(false, OffsetDateTime.parse("2026-05-01T12:00:00Z"), 5, "older owner-of-rider");
        insertReview(true, OffsetDateTime.parse("2026-05-03T12:00:00Z"), 4, "newer rider-of-owner");

        // 2.Exercise
        final Page<ListingPublicReview> page = dao.findListingPublicReviews(LISTING_ID, 0, 10);

        // 3.Assert
        Assertions.assertEquals(2L, page.getTotalItems());
        Assertions.assertEquals(2, page.getContent().size());
        final ListingPublicReview first = page.getContent().get(0);
        Assertions.assertEquals(4, first.getRating());
        Assertions.assertEquals("newer rider-of-owner", first.getComment().orElse(null));
        // For made_by_rider=true, the reviewer name must come from the rider user (Rider Two).
        Assertions.assertEquals("Rider", first.getReviewerForename());
        Assertions.assertEquals("Two", first.getReviewerSurname());
        final ListingPublicReview second = page.getContent().get(1);
        Assertions.assertEquals("Owen", second.getReviewerForename());
        Assertions.assertEquals("Owner", second.getReviewerSurname());
    }

    @Test
    void testFindListingPublicReviewsHonoursPaginationLimitAndOffset() {
        // 1.Arrange
        insertReview(false, OffsetDateTime.parse("2026-05-01T12:00:00Z"), 5, "older");
        insertReview(true, OffsetDateTime.parse("2026-05-03T12:00:00Z"), 4, "newer");

        // 2.Exercise
        final Page<ListingPublicReview> firstPage = dao.findListingPublicReviews(LISTING_ID, 0, 1);
        final Page<ListingPublicReview> secondPage = dao.findListingPublicReviews(LISTING_ID, 1, 1);

        // 3.Assert
        Assertions.assertEquals(2L, firstPage.getTotalItems());
        Assertions.assertEquals(1, firstPage.getContent().size());
        Assertions.assertEquals("newer", firstPage.getContent().get(0).getComment().orElse(null));
        Assertions.assertEquals(2L, secondPage.getTotalItems());
        Assertions.assertEquals(1, secondPage.getContent().size());
        Assertions.assertEquals("older", secondPage.getContent().get(0).getComment().orElse(null));
    }

    @Test
    void testFindRecentCommentReviewsForOwnerCounterpartyIncludesOnlyRiderAuthoredCommentReviews() {
        // 1.Arrange
        insertReview(true, OffsetDateTime.parse("2026-05-01T12:00:00Z"), 5, "Awesome owner");
        insertReview(false, OffsetDateTime.parse("2026-05-02T12:00:00Z"), 5, "Nice rider");

        // 2.Exercise
        final List<ReviewItemDto> recent = dao.findRecentCommentReviewsForCounterparty(OWNER_ID, true, 10);

        // 3.Assert
        Assertions.assertEquals(1, recent.size());
        Assertions.assertEquals(RIDER_ID, recent.get(0).getReviewerUserId());
        Assertions.assertEquals("Rider Two", recent.get(0).getReviewerName());
        Assertions.assertEquals(5, recent.get(0).getRating());
        Assertions.assertEquals("Awesome owner", recent.get(0).getComment().orElse(null));
    }

    @Test
    void testFindRecentCommentReviewsExcludesBlankCommentReviews() {
        // 1.Arrange
        insertReview(true, OffsetDateTime.parse("2026-05-01T12:00:00Z"), 5, "   ");
        insertReview(false, OffsetDateTime.parse("2026-05-02T12:00:00Z"), 5, "");

        // 2.Exercise
        final List<ReviewItemDto> ownerSide = dao.findRecentCommentReviewsForCounterparty(OWNER_ID, true, 10);
        final List<ReviewItemDto> riderSide = dao.findRecentCommentReviewsForCounterparty(RIDER_ID, false, 10);

        // 3.Assert
        Assertions.assertTrue(ownerSide.isEmpty());
        Assertions.assertTrue(riderSide.isEmpty());
    }
}
