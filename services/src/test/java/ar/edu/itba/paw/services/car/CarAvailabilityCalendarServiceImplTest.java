package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.util.CarAvailabilityAddressFormatter;

import ar.edu.itba.paw.services.reservation.ReservationService;
@ExtendWith(MockitoExtension.class)
public class CarAvailabilityCalendarServiceImplTest {

    // Architectural rule: the calendar service no longer touches CarAvailabilityDao;
    // tests mock CarAvailabilityService (the sole DAO owner) instead.
    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private CarAvailabilityAddressFormatter carAvailabilityAddressFormatter;

    @InjectMocks
    private CarAvailabilityCalendarServiceImpl calendarService;

    private static final long CAR_ID = 42L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");
    private static final LocalTime CHECK_IN = LocalTime.of(10, 0);
    private static final LocalTime CHECK_OUT = LocalTime.of(18, 0);

    private static CarAvailability buildAvailabilityWith(
            final long id, final long carId,
            final LocalDate start, final LocalDate end,
            final BigDecimal price, final LocalTime checkIn, final LocalTime checkOut,
            final String street) {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.lenient().when(carRef.getId()).thenReturn(carId);
        return CarAvailability.builder()
                .id(id)
                .car(carRef)
                .startInclusive(start)
                .endInclusive(end)
                .dayPrice(price)
                .startPointStreet(street)
                .checkInTime(checkIn)
                .checkOutTime(checkOut)
                .kind(CarAvailability.Kind.OFFERED)
                .build();
    }

    private static Instant farFutureInstant(final LocalDate baseDay) {
        return ZonedDateTime.of(baseDay.minusDays(10), LocalTime.of(0, 0), AppTimezone.WALL_ZONE).toInstant();
    }

    @Test
    public void testGetBookableSegmentsReturnsEmptyWhenNoAvailabilities() {
        Mockito.when(carAvailabilityService.findByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());

        final List<BookableSegmentProjection> result =
                calendarService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, Instant.now());

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetBookableSegmentsMergesContiguousDaysWithinSameAvailability() {
        // 1. Arrange
        final LocalDate start = LocalDate.of(2030, 9, 1);
        final LocalDate end = LocalDate.of(2030, 9, 6);
        final CarAvailability a = buildAvailabilityWith(
                20L, CAR_ID, start, end, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        Mockito.when(carAvailabilityService.findByCarId(CAR_ID)).thenReturn(List.of(a));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");

        // 2. Act
        final List<BookableSegmentProjection> result =
                calendarService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(start));

        // 3. Assert
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(start, result.get(0).getFrom());
        Assertions.assertEquals(end, result.get(0).getTo());
        Assertions.assertEquals(20L, result.get(0).getAvailabilityId());
    }

    @Test
    public void testGetBookableSegmentsKeepsSeparateWhenContiguousButDifferentAvailabilityId() {
        final LocalDate aStart = LocalDate.of(2030, 9, 1);
        final LocalDate aEnd = LocalDate.of(2030, 9, 3);
        final LocalDate bStart = LocalDate.of(2030, 9, 4);
        final LocalDate bEnd = LocalDate.of(2030, 9, 6);
        final CarAvailability a = buildAvailabilityWith(
                20L, CAR_ID, aStart, aEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        final CarAvailability b = buildAvailabilityWith(
                21L, CAR_ID, bStart, bEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        Mockito.when(carAvailabilityService.findByCarId(CAR_ID)).thenReturn(List.of(a, b));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(b)).thenReturn("Av. Centro");

        final List<BookableSegmentProjection> result =
                calendarService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(aStart));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(20L, result.get(0).getAvailabilityId());
        Assertions.assertEquals(21L, result.get(1).getAvailabilityId());
    }

    @Test
    public void testGetBookableSegmentsKeepsSeparateWhenContiguousButDifferentPrice() {
        final LocalDate aStart = LocalDate.of(2030, 10, 1);
        final LocalDate aEnd = LocalDate.of(2030, 10, 3);
        final LocalDate bStart = LocalDate.of(2030, 10, 4);
        final LocalDate bEnd = LocalDate.of(2030, 10, 6);
        final BigDecimal priceB = new BigDecimal("200.00");
        final CarAvailability a = buildAvailabilityWith(
                30L, CAR_ID, aStart, aEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        final CarAvailability b = buildAvailabilityWith(
                31L, CAR_ID, bStart, bEnd, priceB, CHECK_IN, CHECK_OUT, "Av. Centro");
        Mockito.when(carAvailabilityService.findByCarId(CAR_ID)).thenReturn(List.of(a, b));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(b)).thenReturn("Av. Centro");

        final List<BookableSegmentProjection> result =
                calendarService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(aStart));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(aStart, result.get(0).getFrom());
        Assertions.assertEquals(aEnd, result.get(0).getTo());
        Assertions.assertEquals(PRICE, result.get(0).getDayPrice());
        Assertions.assertEquals(bStart, result.get(1).getFrom());
        Assertions.assertEquals(bEnd, result.get(1).getTo());
        Assertions.assertEquals(priceB, result.get(1).getDayPrice());
    }

    @Test
    public void testGetBookableSegmentsKeepsSeparateWhenContiguousButDifferentCheckInTime() {
        final LocalDate aStart = LocalDate.of(2030, 11, 1);
        final LocalDate aEnd = LocalDate.of(2030, 11, 3);
        final LocalDate bStart = LocalDate.of(2030, 11, 4);
        final LocalDate bEnd = LocalDate.of(2030, 11, 6);
        final CarAvailability a = buildAvailabilityWith(
                40L, CAR_ID, aStart, aEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        final CarAvailability b = buildAvailabilityWith(
                41L, CAR_ID, bStart, bEnd, PRICE, LocalTime.of(8, 0), CHECK_OUT, "Av. Centro");
        Mockito.when(carAvailabilityService.findByCarId(CAR_ID)).thenReturn(List.of(a, b));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");
        Mockito.when(carAvailabilityAddressFormatter.formatPublicPickupLocation(b)).thenReturn("Av. Centro");

        final List<BookableSegmentProjection> result =
                calendarService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(aStart));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(CHECK_IN, result.get(0).getCheckInTime());
        Assertions.assertEquals(LocalTime.of(8, 0), result.get(1).getCheckInTime());
    }
}
