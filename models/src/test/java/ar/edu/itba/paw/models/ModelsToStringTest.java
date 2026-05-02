package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarPicture;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModelsToStringTest {

    @Test
    void testCarToStringIncludesAllFields() {
        // Arrange
        final Car car = Car.builder()
                .id(1L)
                .ownerId(2L)
                .plate("AA123BB")
                .brand("Toyota")
                .model("Yaris")
                .type(Car.Type.HATCHBACK)
                .powertrain(Car.Powertrain.HYBRID)
                .transmission(Car.Transmission.AUTOMATIC)
                .build();
        // Exercise
        final String result = car.toString();
        // Assert
        final String expected = "Car{id=1, ownerId=2, plate='AA123BB', brand='Toyota', model='Yaris', type=HATCHBACK, "
                + "powertrain=HYBRID, transmission=AUTOMATIC}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testUserToStringIncludesAllFields() {
        // Arrange
        final User user = User.identities(1L, "user@example.com", "Ada", "Lovelace");
        // Exercise
        final String result = user.toString();
        // Assert
        final String expected = "User{id=1, email='user@example.com', forename='Ada', surname='Lovelace'}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testListingToStringIncludesAllFields() {
        // Arrange
        final Listing listing = Listing.builder()
                .id(3L)
                .title("Trip")
                .carId(9L)
                .createdAt(OffsetDateTime.parse("2026-04-05T10:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-04-05T11:00:00Z"))
                .status(Listing.Status.ACTIVE)
                .dayPrice(new BigDecimal("150.00"))
                .startPointStreet("Belgrano")
                .description("Description")
                .checkInTime(Listing.DEFAULT_CHECK_IN_TIME)
                .checkOutTime(LocalTime.of(18, 0))
                .build();
        // Exercise
        final String result = listing.toString();
        // Assert
        final String expected = "Listing{id=3, title='Trip', carId=9, createdAt=2026-04-05T10:00Z, updatedAt=2026-04-05T11:00Z, "
                + "status=ACTIVE, dayPrice=150.00, startPointStreet='Belgrano', description='Description', "
                + "checkInTime=10:00, checkOutTime=18:00, ratingAvg=null}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testReservationToStringIncludesAllFields() {
        // Arrange
        final Reservation reservation = Reservation.builder()
                .id(5L)
                .riderId(7L)
                .listingId(11L)
                .startDate(OffsetDateTime.parse("2026-04-10T08:00:00Z"))
                .endDate(OffsetDateTime.parse("2026-04-12T08:00:00Z"))
                .status(Reservation.Status.ACCEPTED)
                .createdAt(OffsetDateTime.parse("2026-04-01T09:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-04-01T10:00:00Z"))
                .totalPrice(new BigDecimal("300.00"))
                .build();
        // Exercise
        final String result = reservation.toString();
        // Assert
        final String expected = "Reservation{id=5, riderId=7, listingId=11, startDate=2026-04-10T08:00Z, endDate=2026-04-12T08:00Z, "
                + "status=ACCEPTED, createdAt=2026-04-01T09:00Z, updatedAt=2026-04-01T10:00Z, totalPrice=300.00, carReturned=false}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testListingAvailabilityToStringIncludesAllFields() {
        // Arrange
        final ListingAvailability availability = new ListingAvailability(
                8L,
                11L,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 25),
                OffsetDateTime.parse("2026-04-05T09:00:00Z"),
                OffsetDateTime.parse("2026-04-05T09:30:00Z"));
        // Exercise
        final String result = availability.toString();
        // Assert
        final String expected = "ListingAvailability{id=8, listingId=11, startInclusive=2026-04-20, endInclusive=2026-04-25, "
                + "createdAt=2026-04-05T09:00Z, updatedAt=2026-04-05T09:30Z}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testCarPictureToStringIncludesAllFields() {
        // Arrange
        final CarPicture picture = new CarPicture(
                1L,
                2L,
                3L,
                0,
                OffsetDateTime.parse("2026-04-05T10:00:00Z"),
                OffsetDateTime.parse("2026-04-05T10:05:00Z"));
        // Exercise
        final String result = picture.toString();
        // Assert
        final String expected = "CarPicture{id=1, carId=2, imageId=3, displayOrder=0, createdAt=2026-04-05T10:00Z, "
                + "updatedAt=2026-04-05T10:05Z}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testImageToStringIncludesAllFieldsIncludingData() {
        // Arrange
        final Image image = new Image(1L, "cover.jpg", "image/jpeg", new byte[] {1, 2, -1});
        // Exercise
        final String result = image.toString();
        // Assert
        final String expected = "Image{id=1, name='cover.jpg', contentType='image/jpeg', data=[1, 2, -1]}";
        Assertions.assertEquals(expected, result);
    }
}

