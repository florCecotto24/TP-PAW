package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.models.domain.Reservation;

public class ListingJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ListingJdbcDao listingDao;

    @Test
    public void testCreateListingPersistsRow() {
        // Arrange
        final OffsetDateTime now = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);

        // Exercise
        final Listing created = listingDao.createListing(
                10L,
                "Title",
                Listing.Status.ACTIVE,
                new BigDecimal("44.50"),
                "Palermo",
                "1234",
                "Great car",
                Listing.DEFAULT_CHECK_IN_TIME,
                LocalTime.of(18, 0),
                null);

        // Assert 
        Assertions.assertTrue(created.getCreatedAt().isAfter(now.minusDays(1)));
        Assertions.assertTrue(created.getId() > 0);
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT title, status, day_price, start_point_street, start_point_number, description,"
                        + " check_in_time, check_out_time FROM listings WHERE id = ?",
                created.getId());
        Assertions.assertEquals("Title", row.get("TITLE"));
        Assertions.assertEquals("active", row.get("STATUS"));
        Assertions.assertEquals(0, new BigDecimal("44.50").compareTo((BigDecimal) row.get("DAY_PRICE")));
        Assertions.assertEquals("Palermo", row.get("START_POINT_STREET"));
        Assertions.assertEquals("1234", row.get("START_POINT_NUMBER"));
        Assertions.assertEquals("Great car", row.get("DESCRIPTION"));
        Assertions.assertEquals(
                Listing.DEFAULT_CHECK_IN_TIME,
                ((Time) row.get("CHECK_IN_TIME")).toLocalTime());
        Assertions.assertEquals(LocalTime.of(18, 0), ((Time) row.get("CHECK_OUT_TIME")).toLocalTime());
    }

    @Test
    public void testGetAllListingsOrderedByCreatedAtDesc() {
        // Arrange
        final OffsetDateTime t0 = OffsetDateTime.parse("2026-04-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(101L, 10L, "Old", Listing.Status.ACTIVE, new BigDecimal("10.00"), t0);
        insertListing(102L, 10L, "New", Listing.Status.ACTIVE, new BigDecimal("20.00"), t0.plusDays(1));

        // Exercise
        final List<Listing> all = listingDao.getAllListings();

        // Assert: JDBC order is ground truth; DAO must agree.
        final List<Long> idsOrdered = jdbcTemplate.query(
                "SELECT id FROM listings ORDER BY created_at DESC",
                (rs, rn) -> rs.getLong(1));
        Assertions.assertEquals(List.of(102L, 101L), idsOrdered);
        Assertions.assertEquals(idsOrdered, all.stream().map(Listing::getId).toList());
    }

    @Test
    public void testGetListingDetailByIdMapsOwnerCarPicturesAndAvailability() {
        // Arrange
        final OffsetDateTime base = OffsetDateTime.parse("2026-05-01T10:00:00Z");
        final LocalDate baseDay = LocalDate.parse("2026-05-01");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "L1", Listing.Status.ACTIVE, new BigDecimal("40.00"), base);
        insertImage(1L, "a.png", "image/png", new byte[] {1});
        insertImage(2L, "b.png", "image/png", new byte[] {2});
        insertCarPicture(11L, 10L, 2L, 2, base, base);
        insertCarPicture(12L, 10L, 1L, 1, base, base);
        insertListingAvailability(21L, 100L, baseDay.plusDays(1), baseDay.plusDays(2), base, base);
        insertListingAvailability(22L, 100L, baseDay.plusDays(3), baseDay.plusDays(4), base, base);

        // Exercise
        final Optional<ListingDetail> detail = listingDao.getListingDetailById(100L);

        // Assert
        Assertions.assertTrue(detail.isPresent());
        Assertions.assertEquals("owner@mail.com", detail.get().getOwner().getEmail());
        Assertions.assertEquals(2, detail.get().getPictures().size());
        Assertions.assertEquals(1, detail.get().getPictures().get(0).getDisplayOrder());
        Assertions.assertEquals(2, detail.get().getListingAvailabilities().size());
        Assertions.assertEquals(baseDay.plusDays(1), detail.get().getListingAvailabilities().get(0).getStartInclusive());
    }

    @Test
    public void testSearchListingsAppliesTextTypeAndPriceFilters() {
        // Arrange
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(11L, 1L, "P2", "Tesla", "Model 3", Car.Type.SEDAN, Car.Powertrain.ELECTRIC, Car.Transmission.AUTOMATIC);
        insertListing(100L, 10L, "City car", Listing.Status.ACTIVE, new BigDecimal("0.00"), base, base, "Palermo", "great hatch");
        insertListing(101L, 11L, "Electric ride", Listing.Status.ACTIVE, new BigDecimal("100.00"), base.plusDays(1), base.plusDays(1), "Belgrano", "silent");
        final ListingSearchCriteria criteria = ListingSearchCriteria.builder()
                .query("ford")
                .carTypes(List.of("HATCHBACK"))
                .build();

        // Exercise
        final List<Listing> result = listingDao.searchListings(criteria);

        // Assert
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(100L, result.get(0).getId());
    }

    @Test
    public void testSearchListingsWithAvailabilityExcludesOverlappingReservations() {
        // Arrange
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        final LocalDate baseDay = LocalDate.parse("2026-06-01");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "rider@mail.com", "Rider", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Available", Listing.Status.ACTIVE, new BigDecimal("50.00"), base);
        insertListingAvailability(1L, 100L, baseDay.plusDays(1), baseDay.plusDays(5), base, base);
        insertReservation(1L, 2L, 100L, base.plusDays(2), base.plusDays(3), Reservation.Status.ACCEPTED, base, base);
        final ListingSearchCriteria blockedWindow = ListingSearchCriteria.builder()
                .availabilityRange(
                        Instant.parse("2026-06-02T00:00:00Z"),
                        Instant.parse("2026-06-04T00:00:00Z"))
                .build();
        final ListingSearchCriteria freeWindow = ListingSearchCriteria.builder()
                .availabilityRange(
                        Instant.parse("2026-06-05T00:00:00Z"),
                        Instant.parse("2026-06-06T00:00:00Z"))
                .build();

        // Exercise & Assert
        Assertions.assertTrue(listingDao.searchListings(blockedWindow).isEmpty());
        Assertions.assertEquals(1, listingDao.searchListings(freeWindow).size());
    }

    @Test
    public void testSearchListingCardsPicksFirstImageByDisplayOrder() {
        // Arrange
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Available", Listing.Status.ACTIVE, new BigDecimal("50.00"), base);
        insertImage(1L, "img1.png", "image/png", new byte[] {1});
        insertImage(2L, "img2.png", "image/png", new byte[] {2});
        insertCarPicture(11L, 10L, 2L, 2, base, base);
        insertCarPicture(12L, 10L, 1L, 1, base, base);

        // Exercise
        final var page = listingDao.searchListingCards(ListingSearchCriteria.builder().build());

        // Assert
        Assertions.assertEquals(1, page.getContent().size());
        Assertions.assertEquals(1L, page.getContent().get(0).getImageId());
        Assertions.assertEquals(1L, page.getTotalItems());
    }

    @Test
    public void testGetHomeListingCardsReturnsBothSections() {
        // Arrange
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(11L, 1L, "P2", "VW", "Golf", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "L1", Listing.Status.ACTIVE, new BigDecimal("60.00"), base);
        insertListing(101L, 11L, "L2", Listing.Status.ACTIVE, new BigDecimal("40.00"), base.plusDays(1));
        final LocalDate wall = LocalDate.parse("2026-06-01");
        insertListingAvailability(300L, 100L, wall, wall.plusDays(30), base, base);
        insertListingAvailability(301L, 101L, wall, wall.plusDays(30), base, base);

        // Exercise
        final HomeListingCards home = listingDao.getHomeListingCards(1, wall, null);

        // Assert
        Assertions.assertEquals(1, home.cheapest().size());
        Assertions.assertEquals(1, home.mostRecent().size());
        Assertions.assertEquals(101L, home.cheapest().get(0).getListingId());
        Assertions.assertEquals(101L, home.mostRecent().get(0).getListingId());
    }

    @Test
    public void testFindSimilarListingCardsUsesCarAttributesAndExcludesSameListing() {
        // Arrange
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(11L, 1L, "P2", "VW", "Polo", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(12L, 1L, "P3", "Tesla", "Model 3", Car.Type.SEDAN, Car.Powertrain.ELECTRIC, Car.Transmission.AUTOMATIC);
        insertListing(100L, 10L, "Base", Listing.Status.ACTIVE, new BigDecimal("60.00"), base);
        insertListing(101L, 11L, "Similar", Listing.Status.ACTIVE, new BigDecimal("40.00"), base.plusDays(1));
        insertListing(102L, 12L, "Different", Listing.Status.ACTIVE, new BigDecimal("40.00"), base.plusDays(2));
        final LocalDate wall = LocalDate.parse("2026-06-01");
        insertListingAvailability(302L, 101L, wall, wall.plusDays(30), base, base);

        // Exercise
        final List<ListingCard> similar = listingDao.findSimilarListingCards(100L, 5, wall, null);

        // Assert
        Assertions.assertEquals(1, similar.size());
        Assertions.assertEquals(101L, similar.get(0).getListingId());
    }

    @Test
    public void testGetOwnerListingCardsReturnsOnlyOwnerListingsOrderedByDate() {
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner1@mail.com", "Owner", "One");
        insertUser(2L, "owner2@mail.com", "Owner", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(11L, 2L, "P2", "VW", "Golf", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Mine-old", Listing.Status.ACTIVE, new BigDecimal("60.00"), base);
        insertListing(101L, 10L, "Mine-new", Listing.Status.PAUSED, new BigDecimal("70.00"), base.plusDays(1));
        insertListing(102L, 11L, "Not-mine", Listing.Status.ACTIVE, new BigDecimal("40.00"), base.plusDays(2));

        final var criteria = new OwnerListingSearchCriteria(1L, 0, 8, null, null, null, null, null, null, null, null, "date", "desc", null);
        final var page = listingDao.getOwnerListingCards(criteria);

        Assertions.assertEquals(2, page.getTotalItems());
        Assertions.assertEquals(2, page.getContent().size());
        Assertions.assertEquals(101L, page.getContent().get(0).getListingId());
        Assertions.assertEquals(100L, page.getContent().get(1).getListingId());
        Assertions.assertEquals(Listing.Status.PAUSED, page.getContent().get(0).getStatus().orElse(null));
        Assertions.assertEquals(Listing.Status.ACTIVE, page.getContent().get(1).getStatus().orElse(null));
    }

    @Test
    public void testGetOwnerListingCardsExcludesListingIdFromCountAndRows() {
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner1@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Keep-a", Listing.Status.ACTIVE, new BigDecimal("60.00"), base);
        insertListing(101L, 10L, "Keep-b", Listing.Status.ACTIVE, new BigDecimal("70.00"), base.plusDays(1));
        insertListing(102L, 10L, "Excluded", Listing.Status.ACTIVE, new BigDecimal("80.00"), base.plusDays(2));

        final var criteria = new OwnerListingSearchCriteria(
                1L, 0, 8, List.of("active"), null, null, null, null, null, null, null, "date", "desc", 102L);
        final var page = listingDao.getOwnerListingCards(criteria);

        Assertions.assertEquals(2L, page.getTotalItems());
        Assertions.assertEquals(2, page.getContent().size());
        Assertions.assertTrue(page.getContent().stream().noneMatch(c -> c.getListingId() == 102L));
    }


    @Test
    public void testGetOwnerListingCardsOrdersByRatingDescThenCreatedAtDesc() {
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner1@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(11L, 1L, "P2", "VW", "Golf", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertCar(12L, 1L, "P3", "Toyota", "Corolla", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.AUTOMATIC);
        insertListing(100L, 10L, "Same-rating-older", Listing.Status.ACTIVE, new BigDecimal("50.00"), base);
        insertListing(101L, 11L, "Same-rating-newer", Listing.Status.ACTIVE, new BigDecimal("60.00"), base.plusDays(1));
        insertListing(102L, 12L, "Best-rating", Listing.Status.ACTIVE, new BigDecimal("70.00"), base);

        jdbcTemplate.update("UPDATE listings SET rating_avg = 4.00 WHERE id = 100");
        jdbcTemplate.update("UPDATE listings SET rating_avg = 4.00 WHERE id = 101");
        jdbcTemplate.update("UPDATE listings SET rating_avg = 5.00 WHERE id = 102");

        final var criteria = new OwnerListingSearchCriteria(
                1L,
                0,
                10,
                List.of("active"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "rating",
                "desc",
                null);
        final var page = listingDao.getOwnerListingCards(criteria);

        final List<Long> expectedOrder = jdbcTemplate.query(
                "SELECT l.id FROM listings l JOIN cars c ON c.id = l.car_id WHERE c.owner_id = ? "
                        + "AND LOWER(l.status) IN ('active') ORDER BY l.rating_avg DESC NULLS LAST, l.created_at DESC",
                (rs, rn) -> rs.getLong(1),
                1L);

        Assertions.assertEquals(List.of(102L, 101L, 100L), expectedOrder);
        Assertions.assertEquals(expectedOrder, page.getContent().stream().map(ListingCard::getListingId).toList());
    }

    @Test
    public void testHasListingsByOwner() {
        final OffsetDateTime base = OffsetDateTime.parse("2026-06-01T00:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "other@mail.com", "Other", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Mine", Listing.Status.ACTIVE, new BigDecimal("60.00"), base);

        Assertions.assertTrue(listingDao.hasListingsByOwner(1L));
        Assertions.assertFalse(listingDao.hasListingsByOwner(2L));
    }

    @Test
    public void testCreateListingWithNullTitleFailsByConstraint() {
        // Arrange
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);

        // Exercise & Assert
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> listingDao.createListing(
                        10L,
                        null,
                        Listing.Status.ACTIVE,
                        new BigDecimal("20.00"),
                        "Palermo",
                        "1",
                        "desc",
                        Listing.DEFAULT_CHECK_IN_TIME,
                        LocalTime.of(18, 0),
                        null));
    }
}

