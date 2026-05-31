package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.reservation.RiderReservationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.pagination.PaginationFallbackSizes;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.PaymentReceiptUploadPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;

/**
 * Sanity coverage for the car-centric ReservationServiceImpl: focuses on small pure-logic methods
 * (billable-day math, reservation lookups) that don't depend on the deleted listing-based flows.
 * The car-based reservation flow is exercised end-to-end through controller and integration tests.
 */
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
    private ReservationAvailabilityService reservationAvailabilityService;

    @Mock
    private ListingAvailabilityService listingAvailabilityService;

    @Mock
    private ListingAddressFormatter listingAddressFormatter;

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

    @Mock
    private CarService carService;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void stubPaginationDefaults() {
        Mockito.lenient().when(paginationPolicy.getDefaultPageSize()).thenReturn(PaginationFallbackSizes.UI_PAGE_SIZE);
    }

    @Test
    public void testGetReservationByIdWhenReservationExists() {
        final Reservation reservation = Reservation.builder()
                .id(1L)
                .rider(User.identities(1L, "r@test.com", "R", "Rider"))
                .car(Mockito.mock(Car.class))
                .startDate(START)
                .endDate(END)
                .status(Reservation.Status.ACCEPTED)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .build();
        Mockito.when(reservationDao.getReservationById(1L)).thenReturn(Optional.of(reservation));

        final Optional<Reservation> result = reservationService.getReservationById(1L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(reservation, result.get());
        Assertions.assertEquals(1L, result.get().getId());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, result.get().getStatus());
    }

    @Test
    public void testGetReservationByIdWhenReservationDoesNotExist() {
        Mockito.when(reservationDao.getReservationById(1L)).thenReturn(Optional.empty());

        final Optional<Reservation> result = reservationService.getReservationById(1L);

        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testFindReminderReservationsReturnsDaoListForSameWindow() {
        final OffsetDateTime from = OffsetDateTime.parse("2026-06-02T03:00:00Z");
        final OffsetDateTime to = OffsetDateTime.parse("2026-06-03T03:00:00Z");
        final List<Reservation> expected = List.of(
                Reservation.builder()
                        .id(7L)
                        .rider(User.identities(1L, "r@test.com", "R", "Rider"))
                        .car(Mockito.mock(Car.class))
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
    public void testSubmitRiderReservationByCarThrowsWhenOwnerIsBlocked() {
        // 1. Arrange
        final long carId = 1L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final User blockedOwner = User.builder()
                .id(ownerId).email("o@test.com").forename("Owner").surname("Test")
                .blocked(true)
                .build();
        final Car car = Car.builder()
                .id(carId)
                .owner(blockedOwner)
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));

        // 2. Act + 3. Assert
        final RiderReservationException thrown = Assertions.assertThrows(
                RiderReservationException.class,
                () -> reservationService.submitRiderReservationByCar(
                        riderId, carId, 99L, "2030-06-01T10:00", "2030-06-02T18:00"));
        Assertions.assertEquals(
                ar.edu.itba.paw.exception.MessageKeys.RESERVATION_OWNER_BLOCKED,
                thrown.getMessageCode());
        // The flow must short-circuit before touching downstream services.
        Mockito.verify(reservationDao, Mockito.never())
                .createReservationForCar(Mockito.anyLong(), Mockito.anyLong(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testSweepRefundOverdueAndBlockOwnersBlocksOwnerAndEnqueuesEmailExactlyOnce() {
        // 1. Arrange — two overdue reservations belong to the same owner; one belongs to a different owner.
        final User owner1 = User.builder().id(101L).email("o1@test.com").forename("O1").surname("Owner1").blocked(false).build();
        final User owner2 = User.builder().id(202L).email("o2@test.com").forename("O2").surname("Owner2").blocked(false).build();
        final Car car1 = Car.builder().id(1L).owner(owner1).plate("AAA111")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL).build();
        final Car car1b = Car.builder().id(2L).owner(owner1).plate("AAA222")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL).build();
        final Car car2 = Car.builder().id(3L).owner(owner2).plate("BBB111")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL).build();
        final OffsetDateTime deadline = OffsetDateTime.parse("2026-05-30T00:00:00Z");
        final Reservation r1 = buildOverdueRefundReservation(11L, car1, deadline);
        final Reservation r2 = buildOverdueRefundReservation(12L, car1b, deadline);
        final Reservation r3 = buildOverdueRefundReservation(13L, car2, deadline);
        Mockito.when(reservationDao.findReservationsWithOverdueRefundProof(Mockito.any()))
                .thenReturn(List.of(r1, r2, r3));
        Mockito.when(userService.getUserById(owner1.getId())).thenReturn(Optional.of(owner1));
        Mockito.when(userService.getUserById(owner2.getId())).thenReturn(Optional.of(owner2));
        Mockito.when(userService.resolveMailLocale(Mockito.anyLong())).thenReturn(java.util.Locale.ENGLISH);

        // 2. Act
        reservationService.sweepRefundOverdueAndBlockOwners();

        // 3. Assert — each owner is blocked exactly once and receives a single email payload.
        Mockito.verify(userService).blockUser(owner1.getId());
        Mockito.verify(userService).blockUser(owner2.getId());
        Mockito.verify(userService, Mockito.never()).blockUser(Mockito.longThat(id -> id != owner1.getId() && id != owner2.getId()));
        Mockito.verify(emailService, Mockito.times(2))
                .sendOwnerBlockedEmail(Mockito.any(ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload.class));
    }

    @Test
    public void testSweepRefundOverdueAndBlockOwnersSkipsAlreadyBlockedOwners() {
        // 1. Arrange
        final User alreadyBlocked = User.builder()
                .id(101L).email("o@test.com").forename("O").surname("Owner").blocked(true).build();
        final Car car = Car.builder().id(1L).owner(alreadyBlocked).plate("AAA111")
                .powertrain(Car.Powertrain.GASOLINE).transmission(Car.Transmission.MANUAL).build();
        final Reservation overdue = buildOverdueRefundReservation(7L, car, OffsetDateTime.parse("2026-05-30T00:00:00Z"));
        Mockito.when(reservationDao.findReservationsWithOverdueRefundProof(Mockito.any()))
                .thenReturn(List.of(overdue));
        Mockito.when(userService.getUserById(alreadyBlocked.getId())).thenReturn(Optional.of(alreadyBlocked));

        // 2. Act
        reservationService.sweepRefundOverdueAndBlockOwners();

        // 3. Assert
        Mockito.verify(userService, Mockito.never()).blockUser(Mockito.anyLong());
        Mockito.verify(emailService, Mockito.never())
                .sendOwnerBlockedEmail(Mockito.any(ar.edu.itba.paw.models.email.OwnerBlockedEmailPayload.class));
    }

    private static Reservation buildOverdueRefundReservation(final long id, final Car car, final OffsetDateTime deadline) {
        return Reservation.builder()
                .id(id)
                .rider(User.identities(999L, "r@test.com", "R", "Rider"))
                .car(car)
                .startDate(START).endDate(END)
                .status(Reservation.Status.CANCELLED_BY_OWNER)
                .createdAt(CREATED_AT).updatedAt(UPDATED_AT)
                .totalPrice(TOTAL_PRICE)
                .paymentRefundRequired(true)
                .refundProofDeadlineAt(deadline)
                .build();
    }

    @Test
    public void testSubmitRiderReservationByCarWhenBillableDaysBelowMinimumThrowsException() {
        // 2 wall days (June 1–2, 2030) < minimumRentalDays = 5
        final long carId = 1L;
        final long riderId = 20L;
        final long ownerId = 10L;
        final long availabilityId = 99L;
        final String fromDateTime = "2030-06-01T10:00";
        final String untilDateTime = "2030-06-02T18:00";

        final User owner = User.identities(ownerId, "owner@test.com", "Owner", "Test");
        final User rider = User.identities(riderId, "rider@test.com", "Rider", "Test");
        final Car car = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .minimumRentalDays(5)
                .build();
        final ListingAvailability avRow = ListingAvailability.builder()
                .id(availabilityId)
                .car(car)
                .startInclusive(LocalDate.of(2030, 5, 1))
                .endInclusive(LocalDate.of(2030, 7, 31))
                .dayPrice(new BigDecimal("100.00"))
                .startPointStreet("Av. Test")
                .checkInTime(LocalTime.of(10, 0))
                .checkOutTime(LocalTime.of(18, 0))
                .kind(ListingAvailability.Kind.OFFERED)
                .build();

        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));
        Mockito.when(userService.getUserById(riderId)).thenReturn(Optional.of(rider));
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        Mockito.when(listingAvailabilityService.findById(availabilityId)).thenReturn(Optional.of(avRow));
        Mockito.when(listingAvailabilityService.findEffectiveForDayByCar(carId, LocalDate.of(2030, 6, 1)))
                .thenReturn(Optional.of(avRow));
        Mockito.when(listingAvailabilityService.findEffectiveForDayByCar(carId, LocalDate.of(2030, 6, 2)))
                .thenReturn(Optional.of(avRow));
        Mockito.when(userService.hasUploadedLicenseAndIdentity(rider)).thenReturn(true);
        Mockito.when(userService.getUserCbu(ownerId)).thenReturn("12345678901234567890123");
        Mockito.lenient().when(reservationTimingPolicy.getMaxBillableDaysPerReservation()).thenReturn(30);
        Mockito.lenient().when(reservationTimingPolicy.getPaymentProofDeadlineHours()).thenReturn(24);

        Assertions.assertThrows(RiderReservationException.class,
                () -> reservationService.submitRiderReservationByCar(
                        riderId, carId, availabilityId, fromDateTime, untilDateTime));
    }

}
