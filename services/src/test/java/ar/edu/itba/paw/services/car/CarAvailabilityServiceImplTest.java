package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import ar.edu.itba.paw.persistence.car.CarAvailabilityDao;
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

    @SuppressWarnings("unused")
    private static OffsetDateTime utc(final String iso) {
        return OffsetDateTime.parse(iso).withOffsetSameInstant(ZoneOffset.UTC);
    }
}
