package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarPicture;
import ar.edu.itba.paw.models.domain.file.Image;
import ar.edu.itba.paw.models.domain.car.CarAvailability;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.user.User;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ModelsToStringTest {

    @Test
    void testCarToStringIncludesAllFields() {
        // Arrange: no carModel set → brand/model/type all delegate to null
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("AA123BB")
                .year(2020)
                .powertrain(Car.Powertrain.HYBRID)
                .transmission(Car.Transmission.AUTOMATIC)
                .status(Car.Status.ACTIVE)
                .createdAt(OffsetDateTime.parse("2026-04-05T10:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-04-05T11:00:00Z"))
                .build();
        // Exercise
        final String result = car.toString();
        // Assert
        final String expected = "Car{id=1, ownerId=2, plate='AA123BB', brand='null', model='null', type=null, "
                + "year=2020, "
                + "powertrain=HYBRID, transmission=AUTOMATIC, status=ACTIVE, "
                + "createdAt=2026-04-05T10:00Z, updatedAt=2026-04-05T11:00Z, ratingAvg=null}";
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
    void testReservationToStringIncludesAllFields() {
        // Arrange
        final Car carRef = Mockito.mock(Car.class);
        Mockito.when(carRef.getId()).thenReturn(3L);
        final Reservation reservation = Reservation.builder()
                .id(5L)
                .rider(User.identities(7L, "r@test.com", "R", "R"))
                .car(carRef)
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
        final String expected = "Reservation{id=5, riderId=7, carId=3, startDate=2026-04-10T08:00Z, endDate=2026-04-12T08:00Z, "
                + "status=ACCEPTED, createdAt=2026-04-01T09:00Z, updatedAt=2026-04-01T10:00Z, totalPrice=300.00, carReturned=false, carReturnedAt=null}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testCarAvailabilityToStringIncludesAllFields() {
        // Arrange
        final Car laCarRef = Mockito.mock(Car.class);
        Mockito.when(laCarRef.getId()).thenReturn(11L);
        final CarAvailability availability = CarAvailability.builder()
                .id(8L)
                .car(laCarRef)
                .startInclusive(LocalDate.of(2026, 4, 20))
                .endInclusive(LocalDate.of(2026, 4, 25))
                .createdAt(OffsetDateTime.parse("2026-04-05T09:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-04-05T09:30:00Z"))
                .build();
        // Exercise
        final String result = availability.toString();
        // Assert
        final String expected = "CarAvailability{id=8, carId=11, startInclusive=2026-04-20, endInclusive=2026-04-25, "
                + "dayPrice=null, kind=OFFERED, createdAt=2026-04-05T09:00Z, updatedAt=2026-04-05T09:30Z}";
        Assertions.assertEquals(expected, result);
    }

    @Test
    void testCarPictureToStringIncludesAllFields() {
        // Arrange
        final Car cpCarRef = Mockito.mock(Car.class);
        Mockito.when(cpCarRef.getId()).thenReturn(2L);
        final CarPicture picture = new CarPicture(
                1L,
                cpCarRef,
                new Image(3L, "img.jpg", "image/jpeg", new byte[0]),
                0,
                OffsetDateTime.parse("2026-04-05T10:00:00Z"),
                OffsetDateTime.parse("2026-04-05T10:05:00Z"));
        // Exercise
        final String result = picture.toString();
        // Assert
        final String expected = "CarPicture{id=1, carId=2, imageId=3, storedFileId=null, displayOrder=0, createdAt=2026-04-05T10:00Z, "
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
