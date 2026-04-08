package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.persistence.ReservationDao;

@ExtendWith(MockitoExtension.class)
public class ListingServiceImplTest {

    @Mock
    private ListingDao listingDao;

    @Mock
    private ListingAvailabilityDao listingAvailabilityDao;

    @SuppressWarnings("unused")
    @Mock
    private CarDao carDao;

    @Mock
    private ReservationDao reservationDao;

    @InjectMocks
    private ListingServiceImpl listingService;

    @Test
    public void testGetListingByIdWhenListingExists() {
        // 1. Arrange
        final long listingId = 1L;
        final long carId = 101L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-02T12:00:00Z");
        final BigDecimal dayPrice = new BigDecimal("50.00");
        final String title = "Test title";
        final String startPoint = "Start";
        final String description = "Desc";
        final LocalTime checkIn = LocalTime.of(9, 0);
        final LocalTime checkOut = LocalTime.of(18, 0);
        final Listing listing = new Listing(
                listingId,
                title,
                carId,
                createdAt,
                updatedAt,
                Listing.Status.ACTIVE,
                dayPrice,
                startPoint,
                description,
                checkIn,
                checkOut);
        Mockito.when(listingDao.getListingById(listingId)).thenReturn(Optional.of(listing));

        // 2. Execute
        final Optional<Listing> result = listingService.getListingById(listingId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(listing, result.get());
    }

    @Test
    public void testGetListingByIdWhenListingDoesNotExist() {
        // 1. Arrange
        final long listingId = 1L;
        Mockito.when(listingDao.getListingById(listingId)).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<Listing> result = listingService.getListingById(listingId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetListingDetailByIdWhenListingExists() {
        // 1. Arrange
        final long listingId = 10L;
        final long carId = 5L;
        final long ownerId = 7L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-02T12:00:00Z");
        final Listing listing = new Listing(
                listingId,
                "Detail title",
                carId,
                createdAt,
                updatedAt,
                Listing.Status.ACTIVE,
                new BigDecimal("40.00"),
                "SP",
                "D",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0));
        final Car car = new Car(carId, ownerId, "ABC", "Brand", "Model", Car.Type.SEDAN, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final User owner = new User(ownerId, "owner@test.com", "O", "User");
        final ListingDetail detail = new ListingDetail(listing, car, owner, List.of(), List.of());
        Mockito.when(listingDao.getListingDetailById(listingId)).thenReturn(Optional.of(detail));

        // 2. Execute
        final Optional<ListingDetail> result = listingService.getListingDetailById(listingId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(detail, result.get());
    }

    @Test
    public void testGetListingDetailByIdWhenListingDoesNotExist() {
        // 1. Arrange
        final long listingId = 99L;
        Mockito.when(listingDao.getListingDetailById(listingId)).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<ListingDetail> result = listingService.getListingDetailById(listingId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAvailabilityByListingId() {
        // 1. Arrange
        final long listingId = 10L;
        final long availabilityId = 1L;
        final LocalDate rangeStart = LocalDate.of(2026, 1, 1);
        final LocalDate rangeEnd = LocalDate.of(2026, 1, 5);
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final ListingAvailability la = new ListingAvailability(availabilityId, listingId, rangeStart, rangeEnd, createdAt, updatedAt);
        final List<ListingAvailability> list = new ArrayList<>();
        list.add(la);
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(list);

        // 2. Execute
        final List<ListingAvailability> result = listingService.findAvailabilityByListingId(listingId);

        // 3. Assert
        Assertions.assertEquals(list, result);
    }

    @Test
    public void testGetBookableWallAvailabilityPeriodsWithoutBlockingReservations() {
        // 1. Arrange
        final long listingId = 10L;
        final long availabilityId = 1L;
        final LocalDate availStart = LocalDate.of(2026, 1, 1);
        final LocalDate availEnd = LocalDate.of(2026, 1, 3);
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final ListingAvailability la = new ListingAvailability(availabilityId, listingId, availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(List.of(la));
        Mockito.when(reservationDao.findBlockingByListingId(listingId)).thenReturn(Collections.emptyList());

        // 2. Execute
        final List<AvailabilityPeriod> periods = listingService.getBookableWallAvailabilityPeriods(listingId);

        // 3. Assert
        Assertions.assertEquals(1, periods.size());
        Assertions.assertEquals(availStart, periods.get(0).getStartInclusive());
        Assertions.assertEquals(availEnd, periods.get(0).getEndInclusive());
    }

    @Test
    public void testGetBookableWallAvailabilityPeriodsSubtractsReservedWallDays() {
        // 1. Arrange
        final long listingId = 10L;
        final long availabilityId = 1L;
        final long reservationId = 1L;
        final long riderId = 2L;
        final LocalDate availStart = LocalDate.of(2026, 1, 1);
        final LocalDate availEnd = LocalDate.of(2026, 1, 3);
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        final ListingAvailability la = new ListingAvailability(availabilityId, listingId, availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(List.of(la));
        final Reservation blocking = new Reservation(
                reservationId,
                riderId,
                listingId,
                LocalDateTime.of(2026, 1, 2, 10, 0).atZone(wall).toOffsetDateTime(),
                LocalDateTime.of(2026, 1, 2, 20, 0).atZone(wall).toOffsetDateTime(),
                Reservation.Status.ACCEPTED,
                createdAt,
                updatedAt);
        Mockito.when(reservationDao.findBlockingByListingId(listingId)).thenReturn(List.of(blocking));

        // 2. Execute
        final List<AvailabilityPeriod> periods = listingService.getBookableWallAvailabilityPeriods(listingId);

        // 3. Assert
        Assertions.assertEquals(2, periods.size());
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 1, 1), periods.get(0).getEndInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 1, 3), periods.get(1).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2026, 1, 3), periods.get(1).getEndInclusive());
    }

    @Test
    public void testGetBookableWallAvailabilityPeriodsWhenThereAreNoAvailability() {
        // 1. Arrange
        final long listingId = 10L;
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(Collections.emptyList());
        Mockito.when(reservationDao.findBlockingByListingId(listingId)).thenReturn(Collections.emptyList());

        // 2. Execute
        final List<AvailabilityPeriod> periods = listingService.getBookableWallAvailabilityPeriods(listingId);

        // 3. Assert
        Assertions.assertTrue(periods.isEmpty());
    }

    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenTheIntervalIsCovered() {
        // 1. Arrange
        final long listingId = 10L;
        final long availabilityId = 1L;
        final LocalDate availStart = LocalDate.of(2026, 2, 1);
        final LocalDate availEnd = LocalDate.of(2026, 2, 28);
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        final ListingAvailability la = new ListingAvailability(availabilityId, listingId, availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(List.of(la));
        Mockito.when(reservationDao.findBlockingByListingId(listingId)).thenReturn(Collections.emptyList());
        final OffsetDateTime pickup = LocalDateTime.of(2026, 2, 5, 10, 0).atZone(wall).toOffsetDateTime();
        final OffsetDateTime dropoff = LocalDateTime.of(2026, 2, 7, 18, 0).atZone(wall).toOffsetDateTime();

        // 2. Execute
        final boolean fits = listingService.reservationIntervalFitsListingAvailability(listingId, null, pickup, dropoff);

        // 3. Assert
        Assertions.assertTrue(fits);
    }

    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenTheIntervalIsNotCovered() {
        // 1. Arrange
        final long listingId = 10L;
        final long availabilityId = 1L;
        final LocalDate availStart = LocalDate.of(2026, 3, 1);
        final LocalDate availEnd = LocalDate.of(2026, 3, 5);
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-01-02T10:00:00Z");
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        final ListingAvailability la = new ListingAvailability(availabilityId, listingId, availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(List.of(la));
        Mockito.when(reservationDao.findBlockingByListingId(listingId)).thenReturn(Collections.emptyList());
        final OffsetDateTime pickup = LocalDateTime.of(2026, 3, 4, 10, 0).atZone(wall).toOffsetDateTime();
        final OffsetDateTime dropoff = LocalDateTime.of(2026, 3, 6, 18, 0).atZone(wall).toOffsetDateTime();
        final Long selectedAvailabilityId = 1L;

        // 2. Execute
        final boolean fits = listingService.reservationIntervalFitsListingAvailability(
                listingId,
                selectedAvailabilityId,
                pickup,
                dropoff);

        // 3. Assert
        Assertions.assertFalse(fits);
    }

    @Test
    public void testReservationIntervalFitsListingAvailabilityWhenThereAreNoBookableDays() {
        // 1. Arrange
        final long listingId = 10L;
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        Mockito.when(listingAvailabilityDao.findByListingId(listingId)).thenReturn(Collections.emptyList());
        Mockito.when(reservationDao.findBlockingByListingId(listingId)).thenReturn(Collections.emptyList());
        final OffsetDateTime pickup = LocalDateTime.of(2026, 4, 1, 10, 0).atZone(wall).toOffsetDateTime();
        final OffsetDateTime dropoff = LocalDateTime.of(2026, 4, 2, 18, 0).atZone(wall).toOffsetDateTime();

        // 2. Execute
        final boolean fits = listingService.reservationIntervalFitsListingAvailability(listingId, null, pickup, dropoff);

        // 3. Assert
        Assertions.assertFalse(fits);
    }

    @Test
    public void testGetAllListingsWhenThereAreListings() {
        // 1. Arrange
        final long firstId = 1L;
        final long secondId = 2L;
        final long carId1 = 101L;
        final long carId2 = 102L;
        final OffsetDateTime createdAt1 = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        final OffsetDateTime createdAt2 = OffsetDateTime.parse("2026-06-02T12:00:00Z");
        final OffsetDateTime updatedAt1 = OffsetDateTime.parse("2026-06-03T12:00:00Z");
        final OffsetDateTime updatedAt2 = OffsetDateTime.parse("2026-06-04T12:00:00Z");
        final String title1 = "A";
        final String title2 = "B";
        final String startPoint1 = "s";
        final String startPoint2 = "s2";
        final String description1 = "d";
        final String description2 = "d2";
        final LocalTime checkIn1 = LocalTime.of(9, 0);
        final LocalTime checkIn2 = LocalTime.of(10, 0);
        final LocalTime checkOut1 = LocalTime.of(18, 0);
        final LocalTime checkOut2 = LocalTime.of(19, 0);
        final BigDecimal dayPrice1 = new BigDecimal("10");
        final BigDecimal dayPrice2 = new BigDecimal("20");
        final Listing first = new Listing(
                firstId,
                title1,
                carId1,
                createdAt1,
                updatedAt1,
                Listing.Status.ACTIVE,
                dayPrice1,
                startPoint1,
                description1,
                checkIn1,
                checkOut1);
        final Listing second = new Listing(
                secondId,
                title2,
                carId2,
                createdAt2,
                updatedAt2,
                Listing.Status.PAUSED,
                dayPrice2,
                startPoint2,
                description2,checkIn2,
                checkOut2);
        final List<Listing> list = List.of(first, second);
        Mockito.when(listingDao.getAllListings()).thenReturn(list);

        // 2. Execute
        final List<Listing> result = listingService.getAllListings();

        // 3. Assert
        Assertions.assertEquals(list, result);
    }

    @Test
    public void testGetAllListingsWhenThereAreNoListings() {
        // 1. Arrange
        final List<Listing> empty = Collections.emptyList();
        Mockito.when(listingDao.getAllListings()).thenReturn(empty);

        // 2. Execute
        final List<Listing> result = listingService.getAllListings();

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }
}
