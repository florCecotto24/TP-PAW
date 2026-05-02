package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.ListingDetail;

class ListingDetailTest {

    @Test
    void testConstructorCreatesDefensiveUnmodifiableCopiesOfLists() {
        // Arrange
        final Listing listing = Listing.builder()
                .id(1L)
                .title("Weekend car")
                .carId(2L)
                .createdAt(OffsetDateTime.parse("2026-04-05T10:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-04-05T11:00:00Z"))
                .status(Listing.Status.ACTIVE)
                .dayPrice(new BigDecimal("99.99"))
                .startPointStreet("Palermo")
                .description("Great city car")
                .checkInTime(Listing.DEFAULT_CHECK_IN_TIME)
                .checkOutTime(LocalTime.of(18, 0))
                .build();

        final Car car = Car.builder()
                .id(2L)
                .ownerId(3L)
                .plate("AA123BB")
                .brand("Toyota")
                .model("Yaris")
                .type(Car.Type.HATCHBACK)
                .powertrain(Car.Powertrain.HYBRID)
                .transmission(Car.Transmission.AUTOMATIC)
                .build();
        final User owner = User.identities(3L, "owner@example.com", "Owner", "Surname");

        final List<CarPicture> pictures = new ArrayList<>(List.of(
                new CarPicture(10L, 2L, 100L, 0,
                        OffsetDateTime.parse("2026-04-05T10:00:00Z"),
                        OffsetDateTime.parse("2026-04-05T10:10:00Z"))));

        final List<ListingAvailability> availabilities = new ArrayList<>(List.of(
                new ListingAvailability(20L, 1L,
                        LocalDate.of(2026, 4, 10),
                        LocalDate.of(2026, 4, 12),
                        OffsetDateTime.parse("2026-04-05T10:00:00Z"),
                        OffsetDateTime.parse("2026-04-05T10:10:00Z"))));

        final ListingDetail detail = new ListingDetail(listing, car, owner, pictures, availabilities);

        // Exercise
        pictures.add(new CarPicture(11L, 2L, 101L, 1,
                OffsetDateTime.parse("2026-04-05T10:20:00Z"),
                OffsetDateTime.parse("2026-04-05T10:30:00Z")));
        availabilities.clear();

        // Assert
        Assertions.assertEquals(1, detail.getPictures().size());
        Assertions.assertEquals(1, detail.getListingAvailabilities().size());
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> detail.getPictures().add(pictures.get(0)));
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> detail.getListingAvailabilities().clear());
    }
}
