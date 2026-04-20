package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ReservationDao;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceImplTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-05T18:00:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("200");

    @Mock
    private ReservationDao reservationDao;

    @Mock
    private ListingService listingService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReservationServiceImpl reservationService;



    @Test
    public void testCreateReservationWhenNoOverlapReturnsCreatedReservation() {
        // 1. Arrange
        final long riderId = 1L;
        final long listingId = 2L;
        final Listing listing = Mockito.mock(Listing.class);
        final Reservation created = new Reservation(
                10L, riderId, listingId, START, END, Reservation.Status.ACCEPTED, CREATED_AT, UPDATED_AT, TOTAL_PRICE);

        Mockito.when(reservationDao.hasActiveOverlap(listingId, START, END)).thenReturn(false);
        Mockito.when(userService.getListingOwner(listingId))
                .thenReturn(Optional.of(new User(99L, "owner@example.com", "O", "Owner")));
        Mockito.when(listing.getDayPrice()).thenReturn(new BigDecimal("40.00"));
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(reservationDao.createReservation(
                riderId, listingId, START, END, Reservation.Status.ACCEPTED, TOTAL_PRICE)).thenReturn(created);

        // 2. Execute
        final Reservation result = reservationService.createReservation(
                riderId, listingId, START, END, Reservation.Status.ACCEPTED);

        // 3. Assert
        Assertions.assertEquals(10L, result.getId());
        Assertions.assertEquals(riderId, result.getRiderId());
        Assertions.assertEquals(listingId, result.getListingId());
        Assertions.assertEquals(START, result.getStartDate());
        Assertions.assertEquals(END, result.getEndDate());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.getStatus());
    }

    @Test
    public void testCreateReservationWhenOverlapThrowsReservationConflictException() {
        // 1. Arrange
        Mockito.when(reservationDao.hasActiveOverlap(2L, START, END)).thenReturn(true);

        // 2. Execute
        final ReservationConflictException thrown = Assertions.assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(1L, 2L, START, END, Reservation.Status.ACCEPTED));

        // 3. Assert
        Assertions.assertEquals(MessageKeys.RESERVATION_CONFLICT_OVERLAP, thrown.getMessageCode());
    }

    @Test
    public void testCreateReservationWhenRiderIsListingOwnerThrowsRiderReservationException() {
        final long riderId = 1L;
        final long listingId = 2L;
        Mockito.when(reservationDao.hasActiveOverlap(listingId, START, END)).thenReturn(false);
        Mockito.when(userService.getListingOwner(listingId))
                .thenReturn(Optional.of(new User(riderId, "same@example.com", "S", "Same")));

        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.createReservation(
                        riderId, listingId, START, END, Reservation.Status.ACCEPTED));

        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING, thrown.getMessageCode());
        Mockito.verify(reservationDao, Mockito.never()).createReservation(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void submitRiderReservationWhenListingNotFoundThrowsRiderReservationException() {
        // 1. Arrange
        final long listingId = 99L;
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.empty());

        // 2. Execute
        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.submitRiderReservation(
                        1L,
                        listingId,
                        null,
                        "2026-06-01T10:00",
                        "2026-06-05T18:00"));

        // 3. Assert
        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_LISTING_NOT_FOUND, thrown.getMessageCode());
    }

    @Test
    public void testSubmitRiderReservationWhenRiderAccountMissingThrowsRiderReservationException() {
        // 1. Arrange
        final long listingId = 2L;
        final long riderId = 99L;
        final Listing listing = Mockito.mock(Listing.class);
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.empty());

        // 2. Execute and 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.submitRiderReservation(
                        riderId,
                        listingId,
                        null,
                        "2026-06-01T10:00",
                        "2026-06-05T18:00"));

        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_USER_NOT_FOUND, thrown.getMessageCode());
    }

    @Test
    public void testSubmitRiderReservationWhenValidReturnsReservation() {
        // 1. Arrange
        final long listingId = 2L;
        final long riderId = 42L;
        final long ownerId = 7L;
        final long reservationId = 100L;

        final String riderEmail = "rider@test.com";
        final String riderName = "RiderName";
        final String riderSurname = "RiderSurname";
        final String ownerEmail = "owner@test.com";
        final String ownerName = "OwnerName";
        final String ownerSurname = "OwnerSurname";

        final Listing listing = Mockito.mock(Listing.class);
        final User createdRider = new User(riderId, riderEmail, riderName, riderSurname);
        final User listingOwner = new User(ownerId, ownerEmail, ownerName, ownerSurname);

        Mockito.when(listing.getStartPoint()).thenReturn("Start point");
        Mockito.when(listing.getTitle()).thenReturn("Test vehicle");
        Mockito.when(listing.getDayPrice()).thenReturn(new BigDecimal("40.00"));

        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(createdRider));

        Mockito.when(listingService.reservationIntervalFitsListingAvailability(
                Mockito.eq(listingId),
                Mockito.isNull(),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(true);

        Mockito.when(userService.getListingOwner(listingId)).thenReturn(Optional.of(listingOwner));

        Mockito.when(reservationDao.hasActiveOverlap(
                Mockito.eq(listingId),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(false);

        Mockito.when(reservationDao.createReservation(
                Mockito.eq(riderId),
                Mockito.eq(listingId),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class),
                Mockito.eq(Reservation.Status.ACCEPTED),
                        Mockito.any(BigDecimal.class)))
                .thenAnswer(inv -> new Reservation(
                        reservationId,
                        createdRider.getId(),
                        listingId,
                        inv.getArgument(2),
                        inv.getArgument(3),
                        Reservation.Status.ACCEPTED,
                        CREATED_AT,
                        UPDATED_AT,
                        TOTAL_PRICE));

        final LocalDate fromDay = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(1);
        final String fromWall = fromDay.atTime(10, 0).toString();
        final String untilWall = fromDay.plusDays(4).atTime(18, 0).toString();

        // 2. Execute
        final Reservation result = reservationService.submitRiderReservation(
                riderId,
                listingId,
                null,
                fromWall,
                untilWall);

        // 3. Assert
        Assertions.assertEquals(reservationId, result.getId());
        Assertions.assertEquals(riderId, result.getRiderId());
        Assertions.assertEquals(listingId, result.getListingId());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.getStatus());
    }

    @Test
    public void testSubmitRiderReservationWhenWallDatesBeforeTodayThrowsRiderReservationException() {
        // 1. Arrange
        final long listingId = 2L;
        final long riderId = 1L;
        final Listing listing = Mockito.mock(Listing.class);
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(new User(riderId, "r@example.com", "R", "Rider")));

        final LocalDate yesterday = LocalDate.now(AvailabilityPeriod.WALL_ZONE).minusDays(1);
        final String fromWall = yesterday.atTime(10, 0).toString();
        final String untilWall = yesterday.plusDays(2).atTime(18, 0).toString();

        // 2. Execute and 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.submitRiderReservation(
                        riderId,
                        listingId,
                        null,
                        fromWall,
                        untilWall));

        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_DATES_NOT_FROM_TODAY, thrown.getMessageCode());
    }

    @Test
    public void submitRiderReservationWhenRiderIsListingOwnerThrowsRiderReservationException() {
        final long listingId = 2L;
        final long sameUserId = 5L;
        final String email = "owner@test.com";
        final Listing listing = Mockito.mock(Listing.class);
        final User ownerAndRider = new User(sameUserId, email, "Owner", "User");

        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(listingService.reservationIntervalFitsListingAvailability(
                Mockito.eq(listingId),
                Mockito.isNull(),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(true);
        Mockito.when(userService.getUserById(sameUserId)).thenReturn(Optional.of(ownerAndRider));
        Mockito.when(userService.getListingOwner(listingId)).thenReturn(Optional.of(ownerAndRider));
        Mockito.when(reservationDao.hasActiveOverlap(
                Mockito.eq(listingId),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(false);

        final LocalDate fromDay = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(1);
        final String fromWall = fromDay.atTime(10, 0).toString();
        final String untilWall = fromDay.plusDays(2).atTime(18, 0).toString();

        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.submitRiderReservation(
                        sameUserId,
                        listingId,
                        null,
                        fromWall,
                        untilWall));

        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING, thrown.getMessageCode());
        Mockito.verify(reservationDao, Mockito.never()).createReservation(
                Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testGetReservationByIdWhenReservationExists() {
        // 1. Arrange
        final Reservation reservation = new Reservation(
                1L, 1L, 1L, START, END, Reservation.Status.ACCEPTED, CREATED_AT, UPDATED_AT, TOTAL_PRICE);
        Mockito.when(reservationDao.getReservationById(1L)).thenReturn(Optional.of(reservation));

        // 2. Execute
        final Optional<Reservation> result = reservationService.getReservationById(1L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(reservation, result.get());
        Assertions.assertEquals(1L, result.get().getId());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.get().getStatus());
    }

    @Test
    public void testGetReservationByIdWhenReservationDoesNotExist() {
        // 1. Arrange
        final long reservationId = 1L;
        Mockito.when(reservationDao.getReservationById(reservationId)).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<Reservation> result = reservationService.getReservationById(reservationId);

        // 3. Assert
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void calculateBillableDaysWhenWallDatesSpanSixteenDaysReturnsSixteen() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-30T18:00");

        final long billableDays = reservationService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(16L, billableDays);
    }

    @Test
    public void calculateBillableDaysWhenCrossingMidnightReturnsTwo() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T23:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-16T01:00");

        final long billableDays = reservationService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(2L, billableDays);
    }

    @Test
    public void calculateTotalWhenWallDatesSpanFifteenDaysReturnsExpectedMoney() {
        final long listingId = 2L;
        final Listing listing = Mockito.mock(Listing.class);
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-30T18:00");

        Mockito.when(listing.getDayPrice()).thenReturn(BigDecimal.valueOf(150L));
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));

        final Optional<BigDecimal> total = reservationService.calculateTotal(listingId, startDate, endDate);

        Assertions.assertTrue(total.isPresent());
        Assertions.assertEquals(0, BigDecimal.valueOf(2400L).compareTo(total.get()));
    }

}
