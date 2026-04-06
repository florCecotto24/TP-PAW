package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ListingDetailTest {

    @Test
    void constructorCreatesDefensiveUnmodifiableCopiesOfLists() {
        // Arrange
        final Listing listing = new Listing(
                1L,
                "Weekend car",
                2L,
                OffsetDateTime.parse("2026-04-05T10:00:00Z"),
                OffsetDateTime.parse("2026-04-05T11:00:00Z"),
                Listing.Status.ACTIVE,
                new BigDecimal("99.99"),
                "Palermo",
                "Great city car",
                LocalTime.of(10, 0),
                LocalTime.of(18, 0));

        final Car car = new Car(2L, 3L, "AA123BB", "Toyota", "Yaris", Car.Type.HATCHBACK,
                Car.Powertrain.HYBRID, Car.Transmission.AUTOMATIC);
        final User owner = new User(3L, "owner@example.com", "Owner", "Surname");

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
