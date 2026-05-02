package ar.edu.itba.paw.services.util;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.services.LocationService;

@ExtendWith(MockitoExtension.class)
public class ListingAddressFormatterTest {

    @Mock
    private LocationService locationService;

    @InjectMocks
    private ListingAddressFormatter listingAddressFormatter;

    private static Listing.Builder baseListing() {
        return Listing.builder()
                .id(1L)
                .title("T")
                .carId(2L)
                .createdAt(OffsetDateTime.parse("2026-01-01T12:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-01-01T12:00:00Z"))
                .status(Listing.Status.ACTIVE)
                .dayPrice(BigDecimal.TEN)
                .description("d")
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(LocalTime.of(18, 0));
    }

    @Test
    public void testFormatPublicPickupLocationJoinsStreetAndNeighborhoodName() {
        final Listing listing = baseListing()
                .startPointStreet("  Corrientes  ")
                .neighborhoodId(9L)
                .build();
        Mockito.when(locationService.findNeighborhoodById(9L))
                .thenReturn(Optional.of(new Neighborhood(9L, "Palermo")));

        Assertions.assertEquals("Corrientes, Palermo", listingAddressFormatter.formatPublicPickupLocation(listing));
    }

    @Test
    public void testFormatPublicPickupLocationWhenNeighborhoodMissingReturnsStreetOnly() {
        final Listing listing = baseListing()
                .startPointStreet("Solo")
                .neighborhoodId(99L)
                .build();
        Mockito.when(locationService.findNeighborhoodById(99L)).thenReturn(Optional.empty());

        Assertions.assertEquals("Solo", listingAddressFormatter.formatPublicPickupLocation(listing));
    }

    @Test
    public void testFormatOwnerReservationHandoverSummaryWhenPickupEqualsDeliveryReturnsSingleLine() {
        final Listing listing = baseListing()
                .startPointStreet("X")
                .startPointNumber("123")
                .neighborhoodId(1L)
                .build();
        Mockito.when(locationService.findNeighborhoodById(1L))
                .thenReturn(Optional.of(new Neighborhood(1L, "N")));

        Assertions.assertEquals("X 123, N", listingAddressFormatter.formatOwnerReservationHandoverSummary(listing));
    }
}
