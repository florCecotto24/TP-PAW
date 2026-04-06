package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.Reservation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

public class ReservationJdbcDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private ReservationJdbcDao reservationDao;

    @Test
    public void testCreateReservationAndGetById() {
        // Arrange
        final OffsetDateTime start = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        final OffsetDateTime end = OffsetDateTime.parse("2026-07-05T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "rider@mail.com", "Rider", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Listing", Listing.Status.ACTIVE, new BigDecimal("30.00"), start.minusDays(20));

        // Exercise
        final Reservation created = reservationDao.createReservation(2L, 100L, start, end, Reservation.Status.ACCEPTED);
        final Optional<Reservation> found = reservationDao.getReservationById(created.getId());

        // Assert
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(2L, found.get().getRiderId());
        Assertions.assertEquals(Reservation.Status.ACCEPTED, found.get().getStatus());
    }

    @Test
    public void testHasActiveOverlap() {
        // Arrange
        final OffsetDateTime created = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "rider@mail.com", "Rider", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Listing", Listing.Status.ACTIVE, new BigDecimal("30.00"), created.minusDays(10));
        insertReservation(
                300L,
                2L,
                100L,
                OffsetDateTime.parse("2026-07-10T00:00:00Z"),
                OffsetDateTime.parse("2026-07-12T00:00:00Z"),
                Reservation.Status.ACCEPTED,
                created,
                created);

        // Exercise & Assert
        Assertions.assertTrue(reservationDao.hasActiveOverlap(
                100L,
                OffsetDateTime.parse("2026-07-11T00:00:00Z"),
                OffsetDateTime.parse("2026-07-13T00:00:00Z")));

        Assertions.assertFalse(reservationDao.hasActiveOverlap(
                100L,
                OffsetDateTime.parse("2026-07-12T00:00:00Z"),
                OffsetDateTime.parse("2026-07-13T00:00:00Z")));
    }

    @Test
    public void testHasActiveOverlapIgnoresCancelledReservation() {
        // Arrange
        final OffsetDateTime created = OffsetDateTime.parse("2026-07-01T10:00:00Z");
        insertUser(1L, "owner@mail.com", "Owner", "One");
        insertUser(2L, "rider@mail.com", "Rider", "Two");
        insertCar(10L, 1L, "P1", "Ford", "Fiesta", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        insertListing(100L, 10L, "Listing", Listing.Status.ACTIVE, new BigDecimal("30.00"), created.minusDays(10));
        insertReservation(
                300L,
                2L,
                100L,
                OffsetDateTime.parse("2026-07-10T00:00:00Z"),
                OffsetDateTime.parse("2026-07-12T00:00:00Z"),
                Reservation.Status.CANCELLED,
                created,
                created);

        // Exercise & Assert
        Assertions.assertFalse(reservationDao.hasActiveOverlap(
                100L,
                OffsetDateTime.parse("2026-07-11T00:00:00Z"),
                OffsetDateTime.parse("2026-07-11T12:00:00Z")));
    }

    @Test
    public void testCreateReservationInvalidForeignKeyFails() {


        // Exercise & Assert
        Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> reservationDao.createReservation(
                        1L,
                        999L,
                        OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                        OffsetDateTime.parse("2026-07-02T10:00:00Z"),
                        Reservation.Status.ACCEPTED));
    }
}

