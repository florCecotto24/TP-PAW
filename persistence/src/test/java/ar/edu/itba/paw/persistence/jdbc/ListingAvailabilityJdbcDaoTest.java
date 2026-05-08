package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.DaoIntegrationTestSupport;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class ListingAvailabilityJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ListingAvailabilityDao listingAvailabilityDao;

    @Test
    public void testCreateListingAvailabilityPersistsRow() {
        // Arrange
        final LocalDate start = LocalDate.parse("2026-05-01");
        final LocalDate end = LocalDate.parse("2026-05-05");
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-04-30T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Listing", Listing.Status.ACTIVE, new BigDecimal("30.00"), createdAt);

        // Exercise
        final ListingAvailability created = listingAvailabilityDao.create(100L, start, end);

        // Assert 
        Assertions.assertEquals(100L, created.getListingId());
        Assertions.assertEquals(start, created.getStartInclusive());
        Assertions.assertEquals(end, created.getEndInclusive());
        final Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT listing_id, start_date, end_date FROM listing_availability WHERE id = ?",
                created.getId());
        Assertions.assertEquals(100L, ((Number) row.get("LISTING_ID")).longValue());
        Assertions.assertEquals(start, ((Date) row.get("START_DATE")).toLocalDate());
        Assertions.assertEquals(end, ((Date) row.get("END_DATE")).toLocalDate());

        final Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM listing_availability WHERE listing_id = ?", Integer.class, 100L);
        Assertions.assertEquals(Integer.valueOf(1), count);
    }

    @Test
    public void testFindByListingIdSortedByStartDate() {
        // Arrange
        final LocalDate base = LocalDate.parse("2026-06-01");
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Listing", Listing.Status.ACTIVE, new BigDecimal("30.00"), createdAt.minusDays(2));
        insertListingAvailability(1L, 100L, base.plusDays(2), base.plusDays(3), createdAt, createdAt);
        insertListingAvailability(2L, 100L, base, base.plusDays(1), createdAt, createdAt);

        // Exercise
        final List<ListingAvailability> availabilities = listingAvailabilityDao.findByListingId(100L);

        // Assert: JDBC ordering is ground truth; DAO result must match.
        final List<LocalDate> startDatesFromDb = jdbcTemplate.query(
                "SELECT start_date FROM listing_availability WHERE listing_id = ? ORDER BY start_date ASC",
                (rs, rn) -> rs.getDate(1).toLocalDate(),
                100L);
        Assertions.assertEquals(List.of(base, base.plusDays(2)), startDatesFromDb);
        Assertions.assertEquals(
                startDatesFromDb,
                availabilities.stream().map(ListingAvailability::getStartInclusive).toList());
    }

    @Test
    public void testCreateWithInvalidListingIdFailsByForeignKey() {
        // Arrange
        final LocalDate start = LocalDate.parse("2026-05-01");

        // Exercise & Assert
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> listingAvailabilityDao.create(999L, start, start.plusDays(1)));
    }
}

