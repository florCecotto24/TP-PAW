package ar.edu.itba.paw.services.car;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.dto.car.CarCard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookableBrowseSupportTest {

    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Test
    void testRetainBookableCardsHidesFullyReservedCars() {
        final CarCard bookable = CarCard.builder().carId(1L).brand("A").model("B").imageId(0L).build();
        final CarCard exhausted = CarCard.builder().carId(2L).brand("C").model("D").imageId(0L).build();

        when(carAvailabilityService.hasRiderBookableSegmentsByCarIds(eq(List.of(1L, 2L)), any()))
                .thenReturn(Map.of(1L, true, 2L, false));

        final List<CarCard> retained = BookableBrowseSupport.retainBookableCards(
                List.of(bookable, exhausted), carAvailabilityService);

        Assertions.assertEquals(List.of(bookable), retained);
    }

    @Test
    void testHasBookableWallDayOnOrAfterRespectsPickupLeadFloor() {
        final LocalDate min = LocalDate.of(2030, 6, 15);
        final List<AvailabilityPeriod> periods = List.of(
                new AvailabilityPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 14)));

        Assertions.assertFalse(BookableBrowseSupport.hasBookableWallDayOnOrAfter(periods, min));
        Assertions.assertTrue(BookableBrowseSupport.hasBookableWallDayOnOrAfter(
                List.of(new AvailabilityPeriod(LocalDate.of(2030, 6, 10), LocalDate.of(2030, 6, 20))),
                min));
    }
}
