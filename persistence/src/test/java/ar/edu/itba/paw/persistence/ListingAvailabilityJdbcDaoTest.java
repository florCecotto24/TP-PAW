package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class ListingAvailabilityJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ListingAvailabilityJdbcDao listingAvailabilityDao;

    @Test
    public void testCreateAndFindByListingId() {
        // Arrange
        final LocalDate start = LocalDate.parse("2026-05-01");
        final LocalDate end = LocalDate.parse("2026-05-05");
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-04-30T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Listing", Listing.Status.ACTIVE, new BigDecimal("30.00"), createdAt);

        // Exercise
        final ListingAvailability created = listingAvailabilityDao.create(100L, start, end);
        final List<ListingAvailability> availabilities = listingAvailabilityDao.findByListingId(100L);

        // Assert
        Assertions.assertEquals(100L, created.getListingId());
        Assertions.assertEquals(1, availabilities.size());
        Assertions.assertEquals(start, availabilities.get(0).getStartInclusive());
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

        // Assert
        Assertions.assertEquals(2, availabilities.size());
        Assertions.assertEquals(base, availabilities.get(0).getStartInclusive());
        Assertions.assertEquals(base.plusDays(2), availabilities.get(1).getStartInclusive());
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

