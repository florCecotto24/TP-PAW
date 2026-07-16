package ar.edu.itba.paw.services.reservation;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.exception.reservation.ReservationConflictException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
import ar.edu.itba.paw.services.car.CarService;
/**
 * Pure date-math coverage for {@link ReservationPricingServiceImpl}; the per-day pricing
 * plan and validation paths are exercised end-to-end via the workflow controller and
 * integration tests.
 */
@ExtendWith(MockitoExtension.class)
class ReservationPricingServiceImplTest {

    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private CarService carService;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationPricingServiceImpl pricingService;

    @Test
    void testCalculateBillableDaysWhenWallDatesSpanSixteenDaysReturnsSixteen() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-30T18:00");

        final long billableDays = pricingService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(16L, billableDays);
    }

    @Test
    void testCalculateBillableDaysWhenCrossingMidnightReturnsTwo() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T23:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-16T01:00");

        final long billableDays = pricingService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(2L, billableDays);
    }

    @Test
    void testCalculateBillableDaysWhenEndNotAfterStartReturnsZero() {
        final OffsetDateTime startDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");
        final OffsetDateTime endDate = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-04-15T10:00");

        final long billableDays = pricingService.calculateBillableDays(startDate, endDate);

        Assertions.assertEquals(0L, billableDays);
    }

    @Test
    void testNormalizeClientReservationTotalAcceptsPlainDigits() {
        Assertions.assertEquals("1234", pricingService.normalizeClientReservationTotal("1234").orElse(null));
    }

    @Test
    void testNormalizeClientReservationTotalAcceptsSingleDecimal() {
        Assertions.assertEquals("1234.56", pricingService.normalizeClientReservationTotal(" 1234.56 ").orElse(null));
    }

    @Test
    void testNormalizeClientReservationTotalRejectsLetters() {
        Assertions.assertTrue(pricingService.normalizeClientReservationTotal("12a").isEmpty());
    }

    @Test
    void testNormalizeClientReservationTotalRejectsBlank() {
        Assertions.assertTrue(pricingService.normalizeClientReservationTotal("   ").isEmpty());
    }

    @Test
    void testNormalizeClientReservationTotalRejectsNull() {
        Assertions.assertTrue(pricingService.normalizeClientReservationTotal(null).isEmpty());
    }

    @Test
    void testReservationIntervalFitsCarAvailabilityRejectsWithdrawnDayInsideOfferedRow() {
        // 1. Arrange — offered row spans the whole month, but one wall day is WITHDRAWN.
        final long carId = 1L;
        final long availabilityId = 99L;
        final Car car = Car.builder()
                .id(carId)
                .owner(User.identities(1L, "owner@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        final CarAvailability offered = CarAvailability.builder()
                .id(availabilityId)
                .car(car)
                .startInclusive(LocalDate.of(2026, 6, 1))
                .endInclusive(LocalDate.of(2026, 6, 30))
                .dayPrice(new BigDecimal("100.00"))
                .kind(CarAvailability.Kind.OFFERED)
                .build();
        final CarAvailability withdrawn = CarAvailability.builder()
                .id(100L)
                .car(car)
                .startInclusive(LocalDate.of(2026, 6, 10))
                .endInclusive(LocalDate.of(2026, 6, 10))
                .dayPrice(new BigDecimal("100.00"))
                .kind(CarAvailability.Kind.WITHDRAWN)
                .build();
        final OffsetDateTime start = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-06-08T10:00");
        final OffsetDateTime end = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2026-06-12T18:00");
        Mockito.when(carAvailabilityService.findById(availabilityId)).thenReturn(Optional.of(offered));
        // Newer rows first (createdAt DESC, id DESC): withdrawn on 10th wins that day.
        Mockito.when(carAvailabilityService.findOverlappingRangeByCar(
                        carId, LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 12)))
                .thenReturn(List.of(withdrawn, offered));

        // 2. Act
        final boolean fits = pricingService.reservationIntervalFitsCarAvailability(
                carId, availabilityId, start, end);

        // 3. Assert
        Assertions.assertFalse(fits);
    }

    @Test
    void testReservationTotalDisplayByCarThrowsWhenIntervalOverlapsBlockingReservation() {
        // 1. Arrange
        final long carId = 1L;
        final User owner = User.identities(1L, "owner@test.com", "O", "O");
        final Car car = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .minimumRentalDays(1)
                .build();
        final CarAvailability offered = CarAvailability.builder()
                .id(10L)
                .car(car)
                .startInclusive(LocalDate.of(2099, 6, 1))
                .endInclusive(LocalDate.of(2099, 6, 30))
                .dayPrice(new BigDecimal("100.00"))
                .kind(CarAvailability.Kind.OFFERED)
                .checkInTime(java.time.LocalTime.of(10, 0))
                .checkOutTime(java.time.LocalTime.of(18, 0))
                .build();
        final OffsetDateTime start = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2099-06-08T10:00");
        final OffsetDateTime end = AvailabilityPeriod.parseWallLocalDateTimeToUtc("2099-06-12T18:00");
        Mockito.when(carService.getCarById(carId)).thenReturn(Optional.of(car));
        Mockito.when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(1);
        Mockito.when(reservationTimingPolicy.getMaxBillableDaysPerReservation()).thenReturn(30);
        Mockito.when(carAvailabilityService.findOverlappingRangeByCar(
                        Mockito.eq(carId), Mockito.any(LocalDate.class), Mockito.any(LocalDate.class)))
                .thenReturn(List.of(offered));
        Mockito.when(carAvailabilityService.findEffectiveForDayByCar(Mockito.eq(carId), Mockito.any(LocalDate.class)))
                .thenReturn(Optional.of(offered));
        Mockito.when(reservationService.hasActiveOverlapByCar(carId, start, end)).thenReturn(true);

        // 2.Act / 3.Assert
        Assertions.assertThrows(
                ReservationConflictException.class,
                () -> pricingService.reservationTotalDisplayByCar(carId, "2099-06-08T10:00", "2099-06-12T18:00"));
    }
}
