package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceImplTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-06-01T10:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-06-05T18:00:00Z");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-05-01T12:00:00Z");
    private static final BigDecimal TOTAL_PRICE = new BigDecimal("200");
    private static final OffsetDateTime PAYMENT_DEADLINE = OffsetDateTime.parse("2026-05-10T12:00:00Z");

    @Mock
    private ReservationDao reservationDao;

    @Mock
    private ListingService listingService;

    @Mock
    private ListingViewService listingViewService;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private PaymentReceiptUploadPolicy paymentReceiptUploadPolicy;

    @Mock
    private PaginationPolicy paginationPolicy;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void stubPaginationDefaults() {
        Mockito.lenient().when(paginationPolicy.getDefaultPageSize()).thenReturn(PaginationFallbackSizes.UI_PAGE_SIZE);
    }

    /** For {@link ReservationServiceImpl#createReservation} (max billable days only). */
    private void stubReservationTimingForReservationFlow() {
        Mockito.when(reservationTimingPolicy.getMaxBillableDaysPerReservation()).thenReturn(365);
    }

    /** For {@link ReservationServiceImpl#submitRiderReservation} (pickup lead, payment deadline, max days). */
    private void stubReservationTimingForSubmitRiderFlow() {
        Mockito.when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(24);
        Mockito.when(reservationTimingPolicy.getPaymentProofDeadlineHours()).thenReturn(12);
        Mockito.when(reservationTimingPolicy.getMaxBillableDaysPerReservation()).thenReturn(365);
    }

    /**
     * Minimal timing stub when {@code submitRiderReservation} exits before proof deadline / {@code createReservation}
     * (e.g. documentation or owner guard).
     */
    private void stubPickupLeadHoursOnlyForSubmitRiderFlow() {
        Mockito.when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(24);
    }

    @Test
    public void testCreateReservationWhenNoOverlapReturnsCreatedReservation() {
        stubReservationTimingForReservationFlow();
        // 1. Arrange
        final long riderId = 1L;
        final long listingId = 2L;
        final Listing listing = Mockito.mock(Listing.class);
        final Reservation created = Reservation.builder()
                .id(10L)
                .riderId(riderId)
                .listingId(listingId)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();

        Mockito.when(reservationDao.hasActiveOverlap(listingId, START, END)).thenReturn(false);
        Mockito.when(userService.getListingOwner(listingId))
                .thenReturn(Optional.of(User.identities(99L, "owner@example.com", "O", "Owner")));
        Mockito.when(userService.getUserCbu(99L)).thenReturn("0170200203000008777719");
        Mockito.when(listing.getDayPrice()).thenReturn(new BigDecimal("40.00"));
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(listingViewService.formatRiderReservationHandoverSummary(Mockito.eq(listing), Mockito.any(Reservation.class)))
                .thenReturn("Rider handover");
        Mockito.when(listingViewService.formatOwnerReservationHandoverSummary(listing)).thenReturn("Owner handover");
        Mockito.doNothing().when(listingService).refreshListingFinishedIfExhausted(listingId);
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(User.identities(riderId, "r@e.com", "R", "Rider")));
        Mockito.when(userService.resolveMailLocale(Mockito.anyLong())).thenReturn(Locale.ENGLISH);
        Mockito.when(reservationDao.createReservation(
                Mockito.eq(riderId),
                Mockito.eq(listingId),
                Mockito.eq(START),
                Mockito.eq(END),
                Mockito.eq(Reservation.Status.ACCEPTED),
                Mockito.any(BigDecimal.class),
                Mockito.eq(PAYMENT_DEADLINE))).thenReturn(created);

        // 2. Execute
        final Reservation result = reservationService.createReservation(
                riderId, listingId, START, END, Reservation.Status.ACCEPTED, PAYMENT_DEADLINE);

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
        stubReservationTimingForReservationFlow();
        // 1. Arrange
        Mockito.when(reservationDao.hasActiveOverlap(2L, START, END)).thenReturn(true);

        // 2. Execute
        final ReservationConflictException thrown = Assertions.assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(1L, 2L, START, END, Reservation.Status.ACCEPTED, null));

        // 3. Assert
        Assertions.assertEquals(MessageKeys.RESERVATION_CONFLICT_OVERLAP, thrown.getMessageCode());
    }

    @Test
    public void testCreateReservationWhenRiderIsListingOwnerThrowsRiderReservationException() {
        stubReservationTimingForReservationFlow();
        final long riderId = 1L;
        final long listingId = 2L;
        Mockito.when(reservationDao.hasActiveOverlap(listingId, START, END)).thenReturn(false);
        Mockito.when(userService.getListingOwner(listingId))
                .thenReturn(Optional.of(User.identities(riderId, "same@example.com", "S", "Same")));

        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.createReservation(
                        riderId, listingId, START, END, Reservation.Status.ACCEPTED, null));

        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_CANNOT_RESERVE_OWN_LISTING, thrown.getMessageCode());
    }

    @Test
    public void testSubmitRiderReservationWhenListingNotFoundThrowsRiderReservationException() {
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
        stubReservationTimingForSubmitRiderFlow();
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
        final User createdRider = User.identities(riderId, riderEmail, riderName, riderSurname);
        final User listingOwner = User.identities(ownerId, ownerEmail, ownerName, ownerSurname);

        Mockito.when(listing.getTitle()).thenReturn("Test vehicle");
        Mockito.when(listing.getDayPrice()).thenReturn(new BigDecimal("40.00"));

        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(listingViewService.formatRiderReservationHandoverSummary(Mockito.eq(listing), Mockito.any(Reservation.class)))
                .thenReturn("Rider handover");
        Mockito.when(listingViewService.formatOwnerReservationHandoverSummary(listing)).thenReturn("Owner handover");
        Mockito.doNothing().when(listingService).refreshListingFinishedIfExhausted(Mockito.anyLong());
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(createdRider));
        Mockito.when(userService.resolveMailLocale(Mockito.anyLong())).thenReturn(Locale.ENGLISH);

        Mockito.when(listingService.reservationIntervalFitsListingAvailability(
                Mockito.eq(listingId),
                Mockito.isNull(),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(true);

        Mockito.when(userService.getListingOwner(listingId)).thenReturn(Optional.of(listingOwner));
        Mockito.when(userService.hasUploadedLicenseAndIdentity(Mockito.any(User.class))).thenReturn(true);
        Mockito.when(userService.getUserCbu(ownerId)).thenReturn("0170200203000008777719");

        Mockito.when(reservationDao.hasActiveOverlap(
                Mockito.eq(listingId),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(false);

        Mockito.when(reservationDao.createReservation(
                Mockito.eq(riderId),
                Mockito.eq(listingId),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class),
                Mockito.eq(Reservation.Status.PENDING),
                Mockito.any(BigDecimal.class),
                Mockito.any(OffsetDateTime.class)))
                .thenAnswer(inv -> Reservation.builder()
                        .id(reservationId)
                        .riderId(createdRider.getId())
                        .listingId(listingId)
                        .startDate(inv.getArgument(2, OffsetDateTime.class))
                        .endDate(inv.getArgument(3, OffsetDateTime.class))
                        .status(Reservation.Status.PENDING)
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .totalPrice(inv.getArgument(5, BigDecimal.class))
                        .paymentProofDeadlineAt(inv.getArgument(6, OffsetDateTime.class))
                        .build());

        /* Takes at least ~48 h in the wall calendar: it complies with the 24 h clock rule even if the test runs at night. */
        final LocalDate fromDay = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(2);
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
        Assertions.assertEquals(Reservation.Status.PENDING, result.getStatus());
    }

    @Test
    public void testSubmitRiderReservationWhenDocumentationMissingThrowsRiderReservationException() {
        stubPickupLeadHoursOnlyForSubmitRiderFlow();
        final long listingId = 2L;
        final long riderId = 42L;
        final long ownerId = 7L;
        final Listing listing = Mockito.mock(Listing.class);
        final User rider = User.identities(riderId, "rider@test.com", "R", "Rider");
        final User listingOwner = User.identities(ownerId, "owner@test.com", "O", "Owner");
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(rider));
        Mockito.when(listingService.reservationIntervalFitsListingAvailability(
                Mockito.eq(listingId),
                Mockito.isNull(),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(true);
        Mockito.when(userService.getListingOwner(listingId)).thenReturn(Optional.of(listingOwner));
        Mockito.when(userService.hasUploadedLicenseAndIdentity(Mockito.any(User.class))).thenReturn(false);

        final LocalDate fromDay = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(2);
        final String fromWall = fromDay.atTime(10, 0).toString();
        final String untilWall = fromDay.plusDays(2).atTime(18, 0).toString();

        final RiderReservationException thrown = Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.submitRiderReservation(
                        riderId,
                        listingId,
                        null,
                        fromWall,
                        untilWall));

        Assertions.assertEquals(MessageKeys.RESERVATION_RIDER_DOCUMENTATION_REQUIRED, thrown.getMessageCode());
    }

    @Test
    public void testSubmitRiderReservationWhenWallDatesBeforeTodayThrowsRiderReservationException() {
        // 1. Arrange
        final long listingId = 2L;
        final long riderId = 1L;
        final Listing listing = Mockito.mock(Listing.class);
        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(User.identities(riderId, "r@example.com", "R", "Rider")));

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
    public void testSubmitRiderReservationWhenRiderIsListingOwnerThrowsRiderReservationException() {
        stubPickupLeadHoursOnlyForSubmitRiderFlow();
        final long listingId = 2L;
        final long sameUserId = 5L;
        final String email = "owner@test.com";
        final Listing listing = Mockito.mock(Listing.class);
        final User ownerAndRider = User.identities(sameUserId, email, "Owner", "User");

        Mockito.when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));
        Mockito.when(listingService.reservationIntervalFitsListingAvailability(
                Mockito.eq(listingId),
                Mockito.isNull(),
                Mockito.any(OffsetDateTime.class),
                Mockito.any(OffsetDateTime.class))).thenReturn(true);
        Mockito.when(userService.getUserById(sameUserId)).thenReturn(Optional.of(ownerAndRider));
        Mockito.when(userService.getListingOwner(listingId)).thenReturn(Optional.of(ownerAndRider));

        final LocalDate fromDay = LocalDate.now(AvailabilityPeriod.WALL_ZONE).plusDays(2);
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
    }

    @Test
    public void testGetReservationByIdWhenReservationExists() {
        // 1. Arrange
        final Reservation reservation = Reservation.builder()
                .id(1L)
                .riderId(1L)
                .listingId(1L)
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
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
    public void testFindReminderReservationsReturnsDaoListForSameWindow() {
        final OffsetDateTime from = OffsetDateTime.parse("2026-06-02T03:00:00Z");
        final OffsetDateTime to = OffsetDateTime.parse("2026-06-03T03:00:00Z");
        final List<Reservation> expected = List.of(
                Reservation.builder()
                        .id(7L)
                        .riderId(1L)
                        .listingId(2L)
                        .startDate(from)
                        .endDate(END)
                        .status(Reservation.Status.ACCEPTED)
                        .createdAt(CREATED_AT)
                        .updatedAt(UPDATED_AT)
                        .totalPrice(TOTAL_PRICE)
                        .build());
        Mockito.when(reservationDao.getReminderReservations(from, to)).thenReturn(expected);

        final List<Reservation> result = reservationService.findReminderReservations(from, to);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testCalculateBillableDaysWhenWallDatesSpanSixteenDaysReturnsSixteen() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-30T18:00");

        final long billableDays = reservationService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(16L, billableDays);
    }

    @Test
    public void testCalculateBillableDaysWhenCrossingMidnightReturnsTwo() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T23:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-16T01:00");

        final long billableDays = reservationService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(2L, billableDays);
    }

    @Test
    public void testCalculateTotalWhenWallDatesSpanFifteenDaysReturnsExpectedMoney() {
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
