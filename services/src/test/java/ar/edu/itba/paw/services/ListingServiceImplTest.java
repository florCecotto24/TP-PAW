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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.dto.ListingPriceMarketInsight;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.services.pagination.ListingBrowsePagination;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.policy.ListingCheckInOutPolicy;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

@ExtendWith(MockitoExtension.class)
public class ListingServiceImplTest {

    @Mock
    private ListingDao listingDao;

    @Mock
    private ListingAvailabilityService listingAvailabilityService;

    @Mock
    private CarService carService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private UserService userService;

    @Mock
    private ImageService imageService;

    @Mock
    private CarPictureService carPictureService;

    @Mock
    private LocationService locationService;

    @Mock
    private EmailService emailService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private ListingCheckInOutPolicy listingCheckInOutPolicy;

    @Mock
    private ListingAvailabilityPolicy listingAvailabilityPolicy;

    @Mock
    private PaginationPolicy paginationPolicy;

    private ListingBrowsePagination listingBrowsePagination;

    private ListingServiceImpl listingService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(paginationPolicy.getUiPageSize()).thenReturn(8);
        Mockito.lenient().when(paginationPolicy.getDbFetchSize()).thenReturn(24);
        Mockito.lenient().when(paginationPolicy.getDefaultPageSize()).thenReturn(8);
        listingBrowsePagination = new ListingBrowsePagination(paginationPolicy);
        listingService = new ListingServiceImpl(
                listingDao,
                listingAvailabilityService,
                carService,
                reservationService,
                userService,
                imageService,
                carPictureService,
                emailService,
                locationService,
                reservationTimingPolicy,
                listingCheckInOutPolicy,
                listingAvailabilityPolicy,
                paginationPolicy,
                listingBrowsePagination);
        Mockito.lenient().when(userService.hasValidCbu(Mockito.any(User.class))).thenAnswer(inv -> {
            final User u = inv.getArgument(0);
            if (u == null) {
                return false;
            }
            return u.getCbu().map(CbuRules::isValidFormat).orElse(false);
        });
    }

    private void stubPublishListingPolicies() {
        Mockito.when(listingCheckInOutPolicy.hasMinimumGap(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.doNothing().when(listingAvailabilityPolicy).validateAvailabilityWithinPublishHorizon(
                Mockito.any(), Mockito.anyList());
    }

    @Test
    public void testGetListingByIdWhenListingExists() {
        // 1. Arrange
        final long listingId = 1L;
        final long carId = 101L;
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-02T12:00:00Z");
        final BigDecimal dayPrice = new BigDecimal("50.00");
        final String title = "Test title";
        final String startPointStreet = "Start";
        final String description = "Desc";
        final LocalTime checkIn = LocalTime.of(9, 0);
        final LocalTime checkOut = LocalTime.of(18, 0);
        final Listing listing = Listing.builder()
                .id(listingId)
                .title(title)
                .car(Mockito.mock(Car.class))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .status(Listing.Status.ACTIVE)
                .dayPrice(dayPrice)
                .startPointStreet(startPointStreet)
                .description(description)
                .checkInTime(checkIn)
                .checkOutTime(checkOut)
                .build();
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
        final User owner = User.identities(ownerId, "owner@test.com", "O", "User");
        final Listing listing = Listing.builder()
                .id(listingId)
                .title("Detail title")
                .car(Mockito.mock(Car.class))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .status(Listing.Status.ACTIVE)
                .dayPrice(new BigDecimal("40.00"))
                .startPointStreet("SP")
                .description("D")
                .checkInTime(LocalTime.of(8, 0))
                .checkOutTime(LocalTime.of(17, 0))
                .build();
        final Car car = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("ABC")
                .brand("Brand")
                .model("Model")
                .type(Car.Type.SEDAN)
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
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
        final Listing laListingRef = Mockito.mock(Listing.class);
        final ListingAvailability la = new ListingAvailability(availabilityId, laListingRef, rangeStart, rangeEnd, createdAt, updatedAt);
        final List<ListingAvailability> list = new ArrayList<>();
        list.add(la);
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(list);

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
        final ListingAvailability la = new ListingAvailability(availabilityId, Mockito.mock(Listing.class), availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(List.of(la));
        Mockito.when(reservationService.findBlockingReservationsByListingId(listingId)).thenReturn(Collections.emptyList());

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
        final ListingAvailability la = new ListingAvailability(availabilityId, Mockito.mock(Listing.class), availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(List.of(la));
        final Reservation blocking = Reservation.builder()
                .id(reservationId)
                .rider(User.identities(riderId, "r@test.com", "R", "Rider"))
                .listing(Mockito.mock(Listing.class))
                .startDate(LocalDateTime.of(2026, 1, 2, 10, 0).atZone(wall).toOffsetDateTime())
                .endDate(LocalDateTime.of(2026, 1, 2, 20, 0).atZone(wall).toOffsetDateTime())
                .status(Reservation.Status.ACCEPTED)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .totalPrice(new BigDecimal("100.00"))
                .build();
        Mockito.when(reservationService.findBlockingReservationsByListingId(listingId)).thenReturn(List.of(blocking));

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
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(Collections.emptyList());
        Mockito.when(reservationService.findBlockingReservationsByListingId(listingId)).thenReturn(Collections.emptyList());

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
        final ListingAvailability la = new ListingAvailability(availabilityId, Mockito.mock(Listing.class), availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(List.of(la));
        Mockito.when(reservationService.findBlockingReservationsByListingId(listingId)).thenReturn(Collections.emptyList());
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
        final ListingAvailability la = new ListingAvailability(availabilityId, Mockito.mock(Listing.class), availStart, availEnd, createdAt, updatedAt);
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(List.of(la));
        Mockito.when(reservationService.findBlockingReservationsByListingId(listingId)).thenReturn(Collections.emptyList());
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
        Mockito.when(listingAvailabilityService.findByListingId(listingId)).thenReturn(Collections.emptyList());
        Mockito.when(reservationService.findBlockingReservationsByListingId(listingId)).thenReturn(Collections.emptyList());
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
        final String startStreet1 = "s";
        final String startStreet2 = "s2";
        final String description1 = "d";
        final String description2 = "d2";
        final LocalTime checkIn1 = LocalTime.of(9, 0);
        final LocalTime checkIn2 = Listing.DEFAULT_CHECK_IN_TIME;
        final LocalTime checkOut1 = LocalTime.of(18, 0);
        final LocalTime checkOut2 = LocalTime.of(19, 0);
        final BigDecimal dayPrice1 = new BigDecimal("10");
        final BigDecimal dayPrice2 = new BigDecimal("20");
        final Listing first = Listing.builder()
                .id(firstId)
                .title(title1)
                .car(Mockito.mock(Car.class))
                .createdAt(createdAt1)
                .updatedAt(updatedAt1)
                .status(Listing.Status.ACTIVE)
                .dayPrice(dayPrice1)
                .startPointStreet(startStreet1)
                .description(description1)
                .checkInTime(checkIn1)
                .checkOutTime(checkOut1)
                .build();
        final Listing second = Listing.builder()
                .id(secondId)
                .title(title2)
                .car(Mockito.mock(Car.class))
                .createdAt(createdAt2)
                .updatedAt(updatedAt2)
                .status(Listing.Status.PAUSED)
                .dayPrice(dayPrice2)
                .startPointStreet(startStreet2)
                .description(description2)
                .checkInTime(checkIn2)
                .checkOutTime(checkOut2)
                .build();
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

    @Test
    public void testGetOwnerListingCardsDelegatesToDao() {
        final List<ListingCard> cards = List.of(new ListingCard(100L, "Ford", "Focus", new BigDecimal("100.00"), 0L));
        final Page<ListingCard> ownerPage = new Page<>(cards, 0, 8, 1);
        Mockito.when(listingDao.getOwnerListingCards(Mockito.any(OwnerListingSearchCriteria.class))).thenReturn(ownerPage);

        final OwnerListingSearchCriteria criteria = new OwnerListingSearchCriteria(5L, 0, 8, null, null, null, null, null, null, null, null, "date", "desc", null);
        final Page<ListingCard> result = listingService.getOwnerListingCards(criteria);

        Assertions.assertEquals(ownerPage, result);
    }

    @Test
    public void testHasListingsByOwnerDelegatesToDao() {
        final long ownerId = 5L;
        Mockito.when(listingDao.hasListingsByOwner(ownerId)).thenReturn(true);

        final boolean result = listingService.hasListingsByOwner(ownerId);

        Assertions.assertTrue(result);
    }

    @Test
    public void testPublishCreatesCarListingAndPictures() {
        stubPublishListingPolicies();
        final long carId = 1L;
        final long ownerId = 2L;
        final long listingId = 3L;
        final long imageId = 4L;
        final long carPictureId = 5L;
        final String plate = "testPlate";
        final String brand = "testBrand";
        final String model = "testModel";
        final Car.Type type = Car.Type.HATCHBACK;
        final Car.Powertrain powertrain = Car.Powertrain.GASOLINE;
        final Car.Transmission transmission = Car.Transmission.MANUAL;
        final BigDecimal pricePerDay = new BigDecimal("100");
        final String expectedTitle = brand + " " + model;
        final String startPointStreet = "testStartPoint";
        final String startPointNumber = "2000";
        final String description = "testDescription";
        final OffsetDateTime createdAt = OffsetDateTime.parse("2026-06-01T12:00:00Z");
        final OffsetDateTime updatedAt = OffsetDateTime.parse("2026-06-02T12:00:00Z");
        final Listing.Status status = Listing.Status.ACTIVE;
        final LocalTime checkInTime = Listing.DEFAULT_CHECK_IN_TIME;
        final LocalTime checkOutTime = LocalTime.of(12, 0);
        final LocalDate startDate = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(1);
        final LocalDate endDate = startDate.plusDays(30);
        final byte[] imageData = {0x00, 0x01, 0x02, 0x03};
        final String imageName = "imageNameTest";
        final String imageType = "contentTypeTest";
        final long neighborhoodId = 1L;

        final Image image = new Image(imageId, imageName, imageType, imageData);
        final User user = User.builder()
                .id(ownerId)
                .email("owner@test.com")
                .forename("ownerName")
                .surname("ownerSurname")
                .cbu("1234567890123456789012")
                .build();
        final Car car = Car.builder()
                .id(carId)
                .owner(user)
                .plate(plate)
                .brand(brand)
                .model(model)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
        final Listing listing = Listing.builder()
                .id(listingId)
                .title(expectedTitle)
                .car(car)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .status(status)
                .dayPrice(pricePerDay)
                .startPointStreet(startPointStreet)
                .startPointNumber(startPointNumber)
                .description(description)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .neighborhood(new Neighborhood(neighborhoodId, "Palermo"))
                .build();
        final CarPicture carPicture = new CarPicture(carPictureId, car, image, 1, createdAt, updatedAt);
        final List<AvailabilityPeriod> periods = List.of(new AvailabilityPeriod(startDate, endDate));
        final List<ImageUpload> uploads = List.of(new ImageUpload(imageName, imageType, imageData));

        Mockito.when(locationService.findNeighborhoodById(neighborhoodId))
                .thenReturn(Optional.of(new Neighborhood(neighborhoodId, "Palermo")));

        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(user));
        Mockito.when(carService.createCar(ownerId, plate, brand, model, type, powertrain, transmission)).thenReturn(car);
        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));
        Mockito.when(listingDao.createListing(
                carId,
                expectedTitle,
                Listing.Status.ACTIVE,
                pricePerDay,
                startPointStreet,
                startPointNumber,
                description,
                checkInTime,
                checkOutTime,
                neighborhoodId)).thenReturn(listing);
        Mockito.when(listingAvailabilityService.create(listingId, startDate, endDate))
                .thenReturn(new ListingAvailability(10L, listing, startDate, endDate, createdAt, updatedAt));
        Mockito.when(imageService.createImage(imageName, imageType, imageData)).thenReturn(image);
        Mockito.when(carPictureService.createCarPicture(carId, image.getId(), 1)).thenReturn(carPicture);

        final CarPublicationResult result = listingService.publish(
                ownerId,
                plate,
                brand,
                model,
                type,
                powertrain,
                transmission,
                pricePerDay,
                startPointStreet,
                startPointNumber,
                description,
                checkInTime,
                checkOutTime,
                periods,
                uploads,
                neighborhoodId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(user, result.getPublisher());
        Assertions.assertEquals(car, result.getCar());
        Assertions.assertEquals(listing, result.getListing());
    }

    @Test
    void getPriceMarketInsightForCar_returnsEmptyWhenCarIsNull() {
        Assertions.assertTrue(listingService.getPriceMarketInsightForCar(null, null).isEmpty());
    }

    @Test
    void getPriceMarketInsightForCar_returnsEmptyWhenBrandOrModelBlank() {
        final User owner = Mockito.mock(User.class);
        final Car car = Car.builder()
                .id(1L)
                .owner(owner)
                .plate("ABC123")
                .brand("  ")
                .model("Gol")
                .type(Car.Type.SEDAN)
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();

        Assertions.assertTrue(listingService.getPriceMarketInsightForCar(car, null).isEmpty());
        Mockito.verifyNoInteractions(listingDao);
    }

    @Test
    void getPriceMarketInsightForCar_delegatesToDaoWithTrimmedBrandAndModel() {
        final User owner = Mockito.mock(User.class);
        final Car car = Car.builder()
                .id(1L)
                .owner(owner)
                .plate("ABC123")
                .brand("  Toyota ")
                .model(" Corolla ")
                .type(Car.Type.SEDAN)
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        final ListingPriceMarketInsight insight = new ListingPriceMarketInsight(
                new BigDecimal("10000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("12500.50"),
                3L);
        Mockito.when(listingDao.findActiveDayPriceMarketInsightByBrandAndModel("Toyota", "Corolla", 42L))
                .thenReturn(Optional.of(insight));

        final Optional<ListingPriceMarketInsight> result = listingService.getPriceMarketInsightForCar(car, 42L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(insight, result.get());
        Mockito.verify(listingDao).findActiveDayPriceMarketInsightByBrandAndModel("Toyota", "Corolla", 42L);
    }

    @Test
    void getPriceMarketInsightForCar_returnsEmptyWhenDaoHasNoData() {
        final User owner = Mockito.mock(User.class);
        final Car car = Car.builder()
                .id(1L)
                .owner(owner)
                .plate("ABC123")
                .brand("Ford")
                .model("Ka")
                .type(Car.Type.HATCHBACK)
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        Mockito.when(listingDao.findActiveDayPriceMarketInsightByBrandAndModel("Ford", "Ka", null))
                .thenReturn(Optional.empty());

        Assertions.assertTrue(listingService.getPriceMarketInsightForCar(car, null).isEmpty());
    }
}
