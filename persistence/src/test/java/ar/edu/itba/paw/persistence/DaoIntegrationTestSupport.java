package ar.edu.itba.paw.persistence;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.Reservation;

@SpringJUnitConfig
@ContextConfiguration(classes = TestPersistenceConfig.class)
public abstract class DaoIntegrationTestSupport {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM car_pictures");
        jdbcTemplate.update("DELETE FROM reservations");
        jdbcTemplate.update("DELETE FROM listing_availability");
        jdbcTemplate.update("DELETE FROM listings");
        jdbcTemplate.update("DELETE FROM cars");
        jdbcTemplate.update("DELETE FROM images");
        jdbcTemplate.update("DELETE FROM email_verification_codes");
        jdbcTemplate.update("DELETE FROM password_reset_codes");
        jdbcTemplate.update("DELETE FROM users");
    }

    protected void insertUser(final long id, final String email, final String forename, final String surname) {
        insertUser(id, email, forename, surname, null);
    }

    protected void insertUser(
            final long id,
            final String email,
            final String forename,
            final String surname,
            final String passwordHash) {
        final Boolean emailValidated = passwordHash != null && !passwordHash.isBlank();
        final java.sql.Date memberSince = java.sql.Date.valueOf(java.time.LocalDate.of(2026, 4, 1));
        jdbcTemplate.update(
                "INSERT INTO users(id, email, forename, surname, password_hash, email_validated, phone_number, birth_date, profile_picture_id, member_since) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, email, forename, surname, passwordHash, emailValidated, null, null, null, memberSince);
    }

    protected void insertCar(
            final long id,
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission) {
        jdbcTemplate.update(
                "INSERT INTO cars(id, owner_id, plate, brand, model, type, transmission, powertrain) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                ownerId,
                plate,
                brand,
                model,
                type.name(),
                transmission.name(),
                powertrain.name());
    }

    protected void insertListing(
            final long id,
            final long carId,
            final String title,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt) {
        insertListing(
                id,
                carId,
                title,
                status,
                dayPrice,
                createdAt,
                createdAt.plusMinutes(1),
                "Palermo",
                "desc",
                LocalTime.of(10, 0),
                LocalTime.of(18, 0));
    }

    protected void insertListing(
            final long id,
            final long carId,
            final String title,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt,
            final String startPointStreet,
            final String description) {
        insertListing(
                id,
                carId,
                title,
                status,
                dayPrice,
                createdAt,
                updatedAt,
                startPointStreet,
                description,
                LocalTime.of(10, 0),
                LocalTime.of(18, 0));
    }

    protected void insertListing(
            final long id,
            final long carId,
            final String title,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt,
            final String startPointStreet,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime) {
        jdbcTemplate.update(
                "INSERT INTO listings(id, title, car_id, created_at, updated_at, status, day_price, start_point_street, "
                        + "start_point_number, description, check_in_time, check_out_time, neighborhood_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                title,
                carId,
                Timestamp.from(createdAt.toInstant()),
                Timestamp.from(updatedAt.toInstant()),
                status.name().toLowerCase(),
                dayPrice,
                startPointStreet,
                "",
                description,
                java.sql.Time.valueOf(checkInTime),
                java.sql.Time.valueOf(checkOutTime),
                22L);
    }

    protected void insertListingAvailability(
            final long id,
            final long listingId,
            final LocalDate startDate,
            final LocalDate endDate,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO listing_availability(id, listing_id, start_date, end_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                listingId,
                Date.valueOf(startDate),
                Date.valueOf(endDate),
                Timestamp.from(createdAt.toInstant()),
                Timestamp.from(updatedAt.toInstant()));
    }

    protected void insertReservation(
            final long id,
            final long riderId,
            final long listingId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final Reservation.Status status,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO reservations(id, rider_id, listing_id, start_date, end_date, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                riderId,
                listingId,
                Timestamp.from(startDate.toInstant()),
                Timestamp.from(endDate.toInstant()),
                status.name().toLowerCase(),
                Timestamp.from(createdAt.toInstant()),
                Timestamp.from(updatedAt.toInstant()));
    }

    protected void insertImage(final long id, final String name, final String contentType, final byte[] data) {
        jdbcTemplate.update("INSERT INTO images(id, image_name, content_type, byte_array) VALUES (?, ?, ?, ?)",
                id, name, contentType, data);
    }

    protected void insertCarPicture(
            final long id,
            final long carId,
            final long imageId,
            final int displayOrder,
            final OffsetDateTime createdAt,
            final OffsetDateTime updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO car_pictures(id, car_id, image_id, display_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                id,
                carId,
                imageId,
                displayOrder,
                Timestamp.from(createdAt.toInstant()),
                Timestamp.from(updatedAt.toInstant()));
    }
}

