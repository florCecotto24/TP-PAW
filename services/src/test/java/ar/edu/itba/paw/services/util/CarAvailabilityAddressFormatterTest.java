package ar.edu.itba.paw.services.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.services.LocationService;

@ExtendWith(MockitoExtension.class)
public class CarAvailabilityAddressFormatterTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private CarAvailabilityAddressFormatterImpl carAvailabilityAddressFormatter;

    private static CarAvailability.Builder baseAvailability() {
        final Car carRef = Mockito.mock(Car.class);
        return CarAvailability.builder()
                .id(1L)
                .car(carRef)
                .startInclusive(LocalDate.of(2026, 1, 1))
                .endInclusive(LocalDate.of(2026, 1, 31))
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(LocalTime.of(18, 0))
                .kind(CarAvailability.Kind.OFFERED);
    }

    @Test
    public void testFormatPublicPickupLocationJoinsStreetAndNeighborhoodName() {
        final CarAvailability availability = baseAvailability()
                .startPointStreet("  Corrientes  ")
                .neighborhood(new Neighborhood(9L, ""))
                .build();
        Mockito.when(locationService.findNeighborhoodById(9L))
                .thenReturn(Optional.of(new Neighborhood(9L, "Palermo")));

        Assertions.assertEquals("Corrientes, Palermo", carAvailabilityAddressFormatter.formatPublicPickupLocation(availability));
    }

    @Test
    public void testFormatPublicPickupLocationWhenNeighborhoodMissingReturnsStreetOnly() {
        final CarAvailability availability = baseAvailability()
                .startPointStreet("Solo")
                .neighborhood(new Neighborhood(99L, ""))
                .build();
        Mockito.when(locationService.findNeighborhoodById(99L)).thenReturn(Optional.empty());

        Assertions.assertEquals("Solo", carAvailabilityAddressFormatter.formatPublicPickupLocation(availability));
    }

    @Test
    public void testFormatOwnerReservationHandoverSummaryWhenPickupEqualsDeliveryReturnsSingleLine() {
        final CarAvailability availability = baseAvailability()
                .startPointStreet("X")
                .startPointNumber("123")
                .neighborhood(new Neighborhood(1L, ""))
                .build();
        Mockito.when(locationService.findNeighborhoodById(1L))
                .thenReturn(Optional.of(new Neighborhood(1L, "N")));

        Assertions.assertEquals("X 123, N", carAvailabilityAddressFormatter.formatOwnerReservationHandoverSummary(availability));
    }
}
