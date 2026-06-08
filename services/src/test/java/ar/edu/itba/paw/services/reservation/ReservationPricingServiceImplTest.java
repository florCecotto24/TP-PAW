package ar.edu.itba.paw.services.reservation;


import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;

import ar.edu.itba.paw.services.car.CarAvailabilityService;
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
}
