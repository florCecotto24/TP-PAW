package ar.edu.itba.paw.services.car;

import java.time.YearMonth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.policy.ReservationTimingPolicy;

import ar.edu.itba.paw.services.location.LocationService;

/**
 * Architectural rules under test:
 *   The search service no longer touches {@code CarDao}; the query/browse methods
 *   (cheapest, most-recent, search, owner cards, similar) moved to {@code CarServiceImpl}.
 *   The search service no longer reads any pagination policy: {@code uiPageSize} is supplied
 *       by the controller layer and threaded through {@code buildSearchCriteria}.
 */
@ExtendWith(MockitoExtension.class)
public class CarSearchServiceImplTest {

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private CarSearchServiceImpl carSearchService;

    @Test
    public void testBuildSearchCriteriaWithFlexibleMonthSetsFlexibleSearchTrue() {
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        final CarSearchCriteria criteria = carSearchService.buildSearchCriteria(
                null, null, null, null, null, null, null,
                null, null, 0, 12, null, null, null,
                true, "2026-06", null);

        Assertions.assertTrue(criteria.isFlexibleSearch());
        Assertions.assertEquals(YearMonth.of(2026, 6), criteria.getFlexibleMonth());
        Assertions.assertNull(criteria.getFlexibleDays());
    }

    @Test
    public void testBuildSearchCriteriaWithFlexibleMonthAndDaysSetsFlexibleDays() {
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        final CarSearchCriteria criteria = carSearchService.buildSearchCriteria(
                null, null, null, null, null, null, null,
                null, null, 0, 12, null, null, null,
                true, "2026-08", 7);

        Assertions.assertTrue(criteria.isFlexibleSearch());
        Assertions.assertEquals(YearMonth.of(2026, 8), criteria.getFlexibleMonth());
        Assertions.assertEquals(Integer.valueOf(7), criteria.getFlexibleDays());
    }

    @Test
    public void testBuildSearchCriteriaWithFlexibleFalseIsNotFlexibleSearch() {
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        final CarSearchCriteria criteria = carSearchService.buildSearchCriteria(
                null, null, null, null, null, null, null,
                null, null, 0, 12, null, null, null,
                false, "2026-06", 3);

        Assertions.assertFalse(criteria.isFlexibleSearch());
        Assertions.assertNull(criteria.getFlexibleMonth());
    }
}
