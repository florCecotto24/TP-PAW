package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
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
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.car.BookableSegmentProjection;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.services.util.ListingAddressFormatter;

@ExtendWith(MockitoExtension.class)
public class ListingAvailabilityServiceImplTest {

    @Mock
    private ListingAvailabilityDao listingAvailabilityDao;

    @Mock
    private ReservationService reservationService;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private ListingAvailabilityPolicy listingAvailabilityPolicy;

    @Mock
    private ListingAddressFormatter listingAddressFormatter;

    @Mock
    private CarService carService;

    @InjectMocks
    private ListingAvailabilityServiceImpl listingAvailabilityService;

    private static final long CAR_ID = 42L;
    private static final BigDecimal PRICE = new BigDecimal("100.00");
    private static final LocalTime CHECK_IN = LocalTime.of(10, 0);
    private static final LocalTime CHECK_OUT = LocalTime.of(18, 0);

    private static ListingAvailability buildAvailability(
            final long id,
            final long carId,
            final LocalDate start,
            final LocalDate end,
            final ListingAvailability.Kind kind) {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.lenient().when(carRef.getId()).thenReturn(carId);
        return ListingAvailability.builder()
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
        final ListingAvailability row = buildAvailability(900L, CAR_ID,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), ListingAvailability.Kind.OFFERED);
        Mockito.when(listingAvailabilityDao.findById(900L)).thenReturn(Optional.of(row));

        final Optional<ListingAvailability> result = listingAvailabilityService.findById(900L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertSame(row, result.get());
    }

    @Test
    public void testFindByCarIdReturnsListFromDao() {
        final ListingAvailability a = buildAvailability(1L, CAR_ID,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 10), ListingAvailability.Kind.OFFERED);
        final ListingAvailability b = buildAvailability(2L, CAR_ID,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 15), ListingAvailability.Kind.OFFERED);
        Mockito.when(listingAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of(a, b));

        final List<ListingAvailability> result = listingAvailabilityService.findByCarId(CAR_ID);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(a, result.get(0));
        Assertions.assertEquals(b, result.get(1));
    }

    @Test
    public void testFindByCarIdsEndingOnOrAfterReturnsListFromDao() {
        final List<Long> ids = List.of(10L, 20L);
        final LocalDate minEnd = LocalDate.of(2026, 7, 1);
        final ListingAvailability row = buildAvailability(3L, 10L,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1), ListingAvailability.Kind.OFFERED);
        Mockito.when(listingAvailabilityDao.findByCarIdsEndingOnOrAfter(ids, minEnd)).thenReturn(List.of(row));

        final List<ListingAvailability> result =
                listingAvailabilityService.findByCarIdsEndingOnOrAfter(ids, minEnd);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(row, result.get(0));
    }


    private static ListingAvailability buildAvailabilityWith(
            final long id, final long carId,
            final LocalDate start, final LocalDate end,
            final BigDecimal price, final LocalTime checkIn, final LocalTime checkOut,
            final String street) {
        final Car carRef = Mockito.mock(Car.class);
        Mockito.lenient().when(carRef.getId()).thenReturn(carId);
        return ListingAvailability.builder()
                .id(id)
                .car(carRef)
                .startInclusive(start)
                .endInclusive(end)
                .dayPrice(price)
                .startPointStreet(street)
                .checkInTime(checkIn)
                .checkOutTime(checkOut)
                .kind(ListingAvailability.Kind.OFFERED)
                .build();
    }

    private static Instant farFutureInstant(final LocalDate baseDay) {
        return ZonedDateTime.of(baseDay.minusDays(10), LocalTime.of(0, 0), AppTimezone.WALL_ZONE).toInstant();
    }

    @Test
    public void testGetBookableSegmentsReturnsEmptyWhenNoAvailabilities() {
        Mockito.when(listingAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());

        final List<BookableSegmentProjection> result =
                listingAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, Instant.now());

        Assertions.assertTrue(result.isEmpty());
    }


    @Test
    public void testGetBookableSegmentsMergesContiguousDaysWithIdenticalProjection() {
        final LocalDate aStart = LocalDate.of(2030, 9, 1);
        final LocalDate aEnd = LocalDate.of(2030, 9, 3);
        final LocalDate bStart = LocalDate.of(2030, 9, 4);
        final LocalDate bEnd = LocalDate.of(2030, 9, 6);
        final ListingAvailability a = buildAvailabilityWith(
                20L, CAR_ID, aStart, aEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        final ListingAvailability b = buildAvailabilityWith(
                21L, CAR_ID, bStart, bEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        Mockito.when(listingAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of(a, b));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        for (LocalDate d = aStart; !d.isAfter(aEnd); d = d.plusDays(1)) {
            Mockito.when(listingAvailabilityDao.findEffectiveForDayByCar(CAR_ID, d)).thenReturn(Optional.of(a));
        }
        for (LocalDate d = bStart; !d.isAfter(bEnd); d = d.plusDays(1)) {
            Mockito.when(listingAvailabilityDao.findEffectiveForDayByCar(CAR_ID, d)).thenReturn(Optional.of(b));
        }
        Mockito.when(listingAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");
        Mockito.when(listingAddressFormatter.formatPublicPickupLocation(b)).thenReturn("Av. Centro");

        final List<BookableSegmentProjection> result =
                listingAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(aStart));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(aStart, result.get(0).getFrom());
        Assertions.assertEquals(bEnd, result.get(0).getTo());
    }

    @Test
    public void testGetBookableSegmentsKeepsSeparateWhenContiguousButDifferentPrice() {
        final LocalDate aStart = LocalDate.of(2030, 10, 1);
        final LocalDate aEnd = LocalDate.of(2030, 10, 3);
        final LocalDate bStart = LocalDate.of(2030, 10, 4);
        final LocalDate bEnd = LocalDate.of(2030, 10, 6);
        final BigDecimal priceB = new BigDecimal("200.00");
        final ListingAvailability a = buildAvailabilityWith(
                30L, CAR_ID, aStart, aEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        final ListingAvailability b = buildAvailabilityWith(
                31L, CAR_ID, bStart, bEnd, priceB, CHECK_IN, CHECK_OUT, "Av. Centro");
        Mockito.when(listingAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of(a, b));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        for (LocalDate d = aStart; !d.isAfter(aEnd); d = d.plusDays(1)) {
            Mockito.when(listingAvailabilityDao.findEffectiveForDayByCar(CAR_ID, d)).thenReturn(Optional.of(a));
        }
        for (LocalDate d = bStart; !d.isAfter(bEnd); d = d.plusDays(1)) {
            Mockito.when(listingAvailabilityDao.findEffectiveForDayByCar(CAR_ID, d)).thenReturn(Optional.of(b));
        }
        Mockito.when(listingAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");
        Mockito.when(listingAddressFormatter.formatPublicPickupLocation(b)).thenReturn("Av. Centro");

        final List<BookableSegmentProjection> result =
                listingAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(aStart));

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
        final ListingAvailability a = buildAvailabilityWith(
                40L, CAR_ID, aStart, aEnd, PRICE, CHECK_IN, CHECK_OUT, "Av. Centro");
        final ListingAvailability b = buildAvailabilityWith(
                41L, CAR_ID, bStart, bEnd, PRICE, LocalTime.of(8, 0), CHECK_OUT, "Av. Centro");
        Mockito.when(listingAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of(a, b));
        Mockito.when(reservationService.findBlockingReservationsByCarId(CAR_ID)).thenReturn(List.of());
        Mockito.when(reservationService.getConfiguredPickupLeadHours()).thenReturn(24);
        for (LocalDate d = aStart; !d.isAfter(aEnd); d = d.plusDays(1)) {
            Mockito.when(listingAvailabilityDao.findEffectiveForDayByCar(CAR_ID, d)).thenReturn(Optional.of(a));
        }
        for (LocalDate d = bStart; !d.isAfter(bEnd); d = d.plusDays(1)) {
            Mockito.when(listingAvailabilityDao.findEffectiveForDayByCar(CAR_ID, d)).thenReturn(Optional.of(b));
        }
        Mockito.when(listingAddressFormatter.formatPublicPickupLocation(a)).thenReturn("Av. Centro");
        Mockito.when(listingAddressFormatter.formatPublicPickupLocation(b)).thenReturn("Av. Centro");

        final List<BookableSegmentProjection> result =
                listingAvailabilityService.getBookableSegmentsForRiderDatePickerByCar(CAR_ID, farFutureInstant(aStart));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(CHECK_IN, result.get(0).getCheckInTime());
        Assertions.assertEquals(LocalTime.of(8, 0), result.get(1).getCheckInTime());
    }

    @Test
    public void testFindOverlappingRangeByCarDelegatesToDao() {
        final LocalDate from = LocalDate.of(2026, 8, 1);
        final LocalDate to = LocalDate.of(2026, 8, 31);
        final ListingAvailability row = buildAvailability(88L, CAR_ID, from, to, ListingAvailability.Kind.OFFERED);
        Mockito.when(listingAvailabilityDao.findOverlappingRangeByCar(CAR_ID, from, to)).thenReturn(List.of(row));

        final List<ListingAvailability> result = listingAvailabilityService.findOverlappingRangeByCar(CAR_ID, from, to);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(row, result.get(0));
    }

    @Test
    public void testUpdateMinimumRentalDaysWhenMinDaysExceedsPeriodThrowsException() {
        final LocalDate start = LocalDate.of(2026, 7, 1);
        final LocalDate end = LocalDate.of(2026, 7, 3); // 3-day period
        final ListingAvailability row = buildAvailability(201L, CAR_ID, start, end, ListingAvailability.Kind.OFFERED);
        Mockito.when(listingAvailabilityDao.findByCarId(CAR_ID)).thenReturn(List.of(row));

        Assertions.assertThrows(CarValidationException.class,
                () -> listingAvailabilityService.updateMinimumRentalDays(CAR_ID, 5));
    }
}
