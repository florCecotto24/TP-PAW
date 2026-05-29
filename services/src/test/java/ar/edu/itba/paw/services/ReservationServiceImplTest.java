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
