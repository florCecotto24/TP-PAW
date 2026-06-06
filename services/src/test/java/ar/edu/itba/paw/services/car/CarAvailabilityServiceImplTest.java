package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.persistence.CarAvailabilityDao;
import ar.edu.itba.paw.policy.CarAvailabilityPolicy;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;

import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.user.UserService;
@ExtendWith(MockitoExtension.class)
public class CarAvailabilityServiceImplTest {

    @Mock
    private CarAvailabilityDao carAvailabilityDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private CarAvailabilityPolicy carAvailabilityPolicy;

    @Mock
    private CarService carService;

    @Mock
    private UserService userService;

    @Mock
    private CarAvailabilityCalendarService carAvailabilityCalendarService;

    @InjectMocks
    private CarAvailabilityServiceImpl carAvailabilityService;

    private static final long CAR_ID = 42L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");
    private static final LocalTime CHECK_IN = LocalTime.of(10, 0);
    private static final LocalTime CHECK_OUT = LocalTime.of(18, 0);

    private static CarAvailability buildAvailability(
            final long id,
            final long carId,
            final LocalDate start,
            final LocalDate end,
            final CarAvailability.Kind kind) {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.lenient().when(carRef.getId()).thenReturn(carId);
        return CarAvailability.builder()
                .id(id)
                .car(carRef)
                .startInclusive(start)
                .endInclusive(end)
                .dayPrice(PRICE)
                .startPointStreet("Street")
                .checkInTime(CHECK_IN)
                .checkOutTime(CHECK_OUT)
                .kind(kind)
                .build();
    }

    @Test
    public void testFindByIdDelegatesToDao() {
        final CarAvailability row = buildAvailability(900L, CAR_ID,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), CarAvailability.Kind.OFFERED);
        Mockito.when(carAvailabilityDao.findById(900L)).thenReturn(Optional.of(row));

        final Optional<CarAvailability> result = carAvailabilityService.findById(900L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(row, result.get());
    }

    @Test
    public void testFindByCarIdReturnsListFromDao() {
        final CarAvailability a = buildAvailability(1L, CAR_ID,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), CarAvailability.Kind.OFFERED);
        final CarAvailability b = buildAvailability(2L, CAR_ID,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), CarAvailability.Kind.OFFERED);
        Mockito.when(carAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of(a, b));

        final List<CarAvailability> result = carAvailabilityService.findByCarId(CAR_ID);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(a, result.get(0));
        Assertions.assertEquals(b, result.get(1));
    }

    @Test
    public void testFindByCarIdsEndingOnOrAfterReturnsListFromDao() {
        final List<Long> ids = List.of(10L, 20L);
        final LocalDate minEnd = LocalDate.of(2026, 7, 1);
        final CarAvailability row = buildAvailability(3L, 10L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1), CarAvailability.Kind.OFFERED);
        Mockito.when(carAvailabilityDao.findByCarIdsEndingOnOrAfter(ids, minEnd)).thenReturn(List.of(row));

        final List<CarAvailability> result =
                carAvailabilityService.findByCarIdsEndingOnOrAfter(ids, minEnd);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(row, result.get(0));
    }

    @Test
    public void testFindOverlappingRangeByCarDelegatesToDao() {
        final LocalDate from = LocalDate.of(2026, 8, 1);
        final LocalDate to = LocalDate.of(2026, 8, 31);
        final CarAvailability row = buildAvailability(88L, CAR_ID, from, to, CarAvailability.Kind.OFFERED);
        Mockito.when(carAvailabilityDao.findOverlappingRangeByCar(CAR_ID, from, to)).thenReturn(List.of(row));

        final List<CarAvailability> result = carAvailabilityService.findOverlappingRangeByCar(CAR_ID, from, to);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(row, result.get(0));
    }

    @Test
    public void testUpdateMinimumRentalDaysWhenMinDaysExceedsPeriodThrowsException() {
        final LocalDate start = LocalDate.of(2026, 7, 1);
        final LocalDate end = LocalDate.of(2026, 7, 3); // 3-day period
        final CarAvailability row = buildAvailability(201L, CAR_ID, start, end, CarAvailability.Kind.OFFERED);
        Mockito.when(carAvailabilityCalendarService.findEffectiveOfferedByCar(CAR_ID))
                .thenReturn(List.of(row));

        Assertions.assertThrows(CarValidationException.class,
                () -> carAvailabilityService.updateMinimumRentalDays(CAR_ID, 5));
    }

    @Test
    public void testFindReservationBlockedWallRangesByCarMapsActiveReservationsToWallRanges() {
        // 1. Arrange — two future-active reservations on the same car.
        final Reservation r1 = Mockito.mock(Reservation.class);
        Mockito.when(r1.getStartDate()).thenReturn(OffsetDateTime.parse("2099-06-05T10:00:00Z"));
        Mockito.when(r1.getEndDate()).thenReturn(OffsetDateTime.parse("2099-06-07T18:00:00Z"));
        final Reservation r2 = Mockito.mock(Reservation.class);
        Mockito.when(r2.getStartDate()).thenReturn(OffsetDateTime.parse("2099-07-10T10:00:00Z"));
        Mockito.when(r2.getEndDate()).thenReturn(OffsetDateTime.parse("2099-07-12T18:00:00Z"));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID))
                .thenReturn(List.of(r1, r2));

        // 2. Act
        final List<AvailabilityPeriod> ranges =
                carAvailabilityService.findReservationBlockedWallRangesByCar(CAR_ID);

        // 3. Assert — each reservation is mapped to its wall start/end day (inclusive).
        Assertions.assertEquals(2, ranges.size());
        Assertions.assertEquals(LocalDate.of(2099, 6, 5), ranges.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2099, 6, 7), ranges.get(0).getEndInclusive());
        Assertions.assertEquals(LocalDate.of(2099, 7, 10), ranges.get(1).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2099, 7, 12), ranges.get(1).getEndInclusive());
    }

    @Test
    public void testFindReservationBlockedWallRangesByCarSkipsReservationsThatAlreadyEnded() {
        // 1. Arrange — one past reservation (ended yesterday in UTC) plus one future-active one.
        // The past one cannot block a new publication, so it must be filtered out.
        final OffsetDateTime longAgoStart = OffsetDateTime.parse("2000-01-01T10:00:00Z");
        final OffsetDateTime longAgoEnd = OffsetDateTime.parse("2000-01-02T18:00:00Z");
        final Reservation past = Mockito.mock(Reservation.class);
        Mockito.when(past.getStartDate()).thenReturn(longAgoStart);
        Mockito.when(past.getEndDate()).thenReturn(longAgoEnd);
        final Reservation future = Mockito.mock(Reservation.class);
        Mockito.when(future.getStartDate()).thenReturn(OffsetDateTime.parse("2099-08-01T10:00:00Z"));
        Mockito.when(future.getEndDate()).thenReturn(OffsetDateTime.parse("2099-08-05T18:00:00Z"));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID))
                .thenReturn(List.of(past, future));

        // 2. Act
        final List<AvailabilityPeriod> ranges =
                carAvailabilityService.findReservationBlockedWallRangesByCar(CAR_ID);

        // 3. Assert — only the future reservation remains.
        Assertions.assertEquals(1, ranges.size());
        Assertions.assertEquals(LocalDate.of(2099, 8, 1), ranges.get(0).getStartInclusive());
        Assertions.assertEquals(LocalDate.of(2099, 8, 5), ranges.get(0).getEndInclusive());
    }

    @Test
    public void testFindReservationBlockedWallRangesByCarReturnsEmptyWhenNoActiveReservations() {
        // 1. Arrange
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID))
                .thenReturn(List.of());

        // 2. Act
        final List<AvailabilityPeriod> ranges =
                carAvailabilityService.findReservationBlockedWallRangesByCar(CAR_ID);

        // 3. Assert
        Assertions.assertTrue(ranges.isEmpty());
    }

    @SuppressWarnings("unused")
    private static OffsetDateTime utc(final String iso) {
        return OffsetDateTime.parse(iso).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
