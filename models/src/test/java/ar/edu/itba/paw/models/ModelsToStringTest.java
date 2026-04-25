package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ModelsToStringTest {

    @Test
    void carToStringIncludesAllFields() {
        // Arrange
        final Car car = new Car(1L, 2L, "AA123BB", "Toyota", "Yaris", Car.Type.HATCHBACK,
                Car.Powertrain.HYBRID, Car.Transmission.AUTOMATIC);
        // Exercise
        final String result = car.toString();
        // Assert
        final String expected = "Car{id=1, ownerId=2, plate='AA123BB', brand='Toyota', model='Yaris', type=HATCHBACK, "
                + "powertrain=HYBRID, transmission=AUTOMATIC}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void userToStringIncludesAllFields() {
        // Arrange
        final User user = new User(1L, "user@example.com", "Ada", "Lovelace");
        // Exercise
        final String result = user.toString();
        // Assert
        final String expected = "User{id=1, email='user@example.com', forename='Ada', surname='Lovelace'}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void listingToStringIncludesAllFields() {
        // Arrange
        final Listing listing = new Listing(
                3L,
                "Trip",
                9L,
                OffsetDateTime.parse("2026-04-05T10:00:00Z"),
                OffsetDateTime.parse("2026-04-05T11:00:00Z"),
                Listing.Status.ACTIVE,
                new BigDecimal("150.00"),
                "Belgrano",
                "Description",
                LocalTime.of(10, 0),
                LocalTime.of(18, 0));
        // Exercise
        final String result = listing.toString();
        // Assert
        final String expected = "Listing{id=3, title='Trip', carId=9, createdAt=2026-04-05T10:00Z, updatedAt=2026-04-05T11:00Z, "
                + "status=ACTIVE, dayPrice=150.00, startPointStreet='Belgrano', description='Description', "
                + "checkInTime=10:00, checkOutTime=18:00}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void reservationToStringIncludesAllFields() {
        // Arrange
        final Reservation reservation = new Reservation(
                5L,
                7L,
                11L,
                OffsetDateTime.parse("2026-04-10T08:00:00Z"),
                OffsetDateTime.parse("2026-04-12T08:00:00Z"),
                Reservation.Status.ACCEPTED,
                OffsetDateTime.parse("2026-04-01T09:00:00Z"),
                OffsetDateTime.parse("2026-04-01T10:00:00Z"),
                new BigDecimal("300.00"));
        // Exercise
        final String result = reservation.toString();
        // Assert
        final String expected = "Reservation{id=5, riderId=7, listingId=11, startDate=2026-04-10T08:00Z, endDate=2026-04-12T08:00Z, "
                + "status=ACCEPTED, createdAt=2026-04-01T09:00Z, updatedAt=2026-04-01T10:00Z, totalPrice=300.00}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void listingAvailabilityToStringIncludesAllFields() {
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
    void carPictureToStringIncludesAllFields() {
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
    void imageToStringIncludesAllFieldsIncludingData() {
        // Arrange
        final Image image = new Image(1L, "cover.jpg", "image/jpeg", new byte[] {1, 2, -1});
        // Exercise
        final String result = image.toString();
        // Assert
        final String expected = "Image{id=1, name='cover.jpg', contentType='image/jpeg', data=[1, 2, -1]}";
        Assertions.assertEquals(expected, result);
    }
}

