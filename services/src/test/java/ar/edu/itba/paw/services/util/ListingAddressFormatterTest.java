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
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.services.LocationService;

@ExtendWith(MockitoExtension.class)
public class ListingAddressFormatterTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private ListingAddressFormatter listingAddressFormatter;

    private static ListingAvailability.Builder baseAvailability() {
        final Car carRef = Mockito.mock(Car.class);
        return ListingAvailability.builder()
                .id(1L)
                .car(carRef)
                .startInclusive(LocalDate.of(2026, 1, 1))
                .endInclusive(LocalDate.of(2026, 1, 31))
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(LocalTime.of(18, 0))
                .kind(ListingAvailability.Kind.OFFERED);
    }

    @Test
    public void testFormatPublicPickupLocationJoinsStreetAndNeighborhoodName() {
        final ListingAvailability availability = baseAvailability()
                .startPointStreet("  Corrientes  ")
                .neighborhood(new Neighborhood(9L, ""))
                .build();
        Mockito.when(locationService.findNeighborhoodById(9L))
                .thenReturn(Optional.of(new Neighborhood(9L, "Palermo")));

        Assertions.assertEquals("Corrientes, Palermo", listingAddressFormatter.formatPublicPickupLocation(availability));
    }

    @Test
    public void testFormatPublicPickupLocationWhenNeighborhoodMissingReturnsStreetOnly() {
        final ListingAvailability availability = baseAvailability()
                .startPointStreet("Solo")
                .neighborhood(new Neighborhood(99L, ""))
                .build();
        Mockito.when(locationService.findNeighborhoodById(99L)).thenReturn(Optional.empty());

        Assertions.assertEquals("Solo", listingAddressFormatter.formatPublicPickupLocation(availability));
    }

    @Test
    public void testFormatOwnerReservationHandoverSummaryWhenPickupEqualsDeliveryReturnsSingleLine() {
        final ListingAvailability availability = baseAvailability()
                .startPointStreet("X")
                .startPointNumber("123")
                .neighborhood(new Neighborhood(1L, ""))
                .build();
        Mockito.when(locationService.findNeighborhoodById(1L))
                .thenReturn(Optional.of(new Neighborhood(1L, "N")));

        Assertions.assertEquals("X 123, N", listingAddressFormatter.formatOwnerReservationHandoverSummary(availability));
    }
}
