package ar.edu.itba.paw.webapp.integration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.DockerClientFactory;

import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.time.AppTimezone;
import ar.edu.itba.paw.persistence.car.CarDao;

/**
 * Only test in the repo that exercises {@code CarJpaDao}'s flexible-search branch
 * ({@code appendFlexibleSearchFilter}: {@code CROSS JOIN LATERAL generate_series} + {@code AT TIME
 * ZONE}), which is PostgreSQL-only syntax and cannot run against the HSQLDB used by every other
 * DAO test (see AUDITORIA-FINAL.md, "PERSIST-TEST"). Runs against a disposable Postgres via
 * Testcontainers, bootstrapped with the exact same baseline + Flyway migrations production uses
 * ({@link PostgresCarSearchTestConfig}), so the schema can never drift from what {@code CarJpaDao}
 * actually queries.
 *
 * <p>Scenario: a car with a whole month {@code OFFERED} and no reservations (plenty of free
 * windows), one fully booked except for a too-short tail (no valid window), and one booked to
 * leave an <em>exactly</em> {@code flexibleDays}-long tail (boundary case) — this exercises the
 * {@code GREATEST}/{@code LEAST}/{@code - (flexibleDays - 1)} window-bound arithmetic, not just
 * "the query doesn't throw".
 *
 * <p>Deliberately named {@code *IT} (not {@code *Test}): Surefire's default include pattern does
 * NOT pick up {@code *IT} classes, so {@code mvn test} / {@code mvn clean install} / {@code mvn
 * clean package} are completely unaffected by this class — it only runs when explicitly requested,
 * so a missing local Docker daemon can never break the standard build. Run it explicitly (Docker
 * must be running) with:
 * <pre>mvn test -pl webapp -am -Dtest=CarFlexibleSearchPostgresIT</pre>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PostgresCarSearchTestConfig.class)
@Transactional
@EnabledIf("dockerAvailable")
class CarFlexibleSearchPostgresIT {

    @Autowired
    private CarDao dao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    static boolean dockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }

    @Test
    void testFlexibleSearchOnlyMatchesCarsWithAFreeWindowLongEnough() {
        final long ownerId = seedUser("flex-search-owner@test.com");
        final long riderId = seedUser("flex-search-rider@test.com");
        final long modelId = seedValidatedModel("FlexBrand", "FlexModel");

        // Car A: whole month free -> plenty of 5-day windows starting anywhere in [03-01, 03-27].
        final long carA = seedCar(ownerId, modelId, "FLEXA01");
        seedOfferedAvailability(carA, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31));

        // Car B: booked [03-01, 03-29) -> only a ~2-day tail is free; no 5-day window fits anywhere.
        final long carB = seedCar(ownerId, modelId, "FLEXB02");
        seedOfferedAvailability(carB, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31));
        seedActiveReservation(carB, riderId, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 29));

        // Car C: booked [03-01, 03-26) -> leaves EXACTLY a 5-day tail (03-27..03-31): boundary case.
        final long carC = seedCar(ownerId, modelId, "FLEXC03");
        seedOfferedAvailability(carC, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31));
        seedActiveReservation(carC, riderId, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 26));

        final CarSearchCriteria criteria = CarSearchCriteria.builder()
                .flexibleMonth(YearMonth.of(2027, 3))
                .flexibleDays(5)
                .page(0)
                .uiPageSize(10)
                .build();

        final Page<CarCard> result = dao.searchCarCards(criteria);
        final Set<Long> matchedIds =
                result.getContent().stream().map(CarCard::getCarId).collect(Collectors.toSet());

        Assertions.assertTrue(matchedIds.contains(carA), "Car A (mes libre) deberia matchear.");
        Assertions.assertFalse(matchedIds.contains(carB), "Car B (sin hueco de 5 dias) NO deberia matchear.");
        Assertions.assertTrue(matchedIds.contains(carC), "Car C (hueco de EXACTO 5 dias, limite) deberia matchear.");
    }

    private long seedUser(final String email) {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                email, "Test", "User");
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private long seedValidatedModel(final String brandName, final String modelName) {
        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, TRUE)", brandName);
        final long brandId =
                jdbcTemplate.queryForObject("SELECT id FROM car_brands WHERE name = ?", Long.class, brandName);
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, TRUE, 'SEDAN')",
                brandId, modelName);
        return jdbcTemplate.queryForObject("SELECT id FROM car_models WHERE name = ?", Long.class, modelName);
    }

    private long seedCar(final long ownerId, final long modelId, final String plate) {
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, plate, transmission, powertrain, status, model_id, "
                        + "minimum_rental_days, created_at, updated_at) "
                        + "VALUES (?, ?, 'MANUAL', 'GASOLINE', 'active', ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                ownerId, plate, modelId);
        return jdbcTemplate.queryForObject("SELECT id FROM cars WHERE plate = ?", Long.class, plate);
    }

    private void seedOfferedAvailability(final long carId, final LocalDate start, final LocalDate end) {
        jdbcTemplate.update(
                "INSERT INTO car_availability (car_id, start_date, end_date, created_at, updated_at, "
                        + "day_price, start_point_street, check_in_time, check_out_time, kind) "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 'Main St', "
                        + "'10:00', '10:00', 'offered')",
                carId, java.sql.Date.valueOf(start), java.sql.Date.valueOf(end));
    }

    private void seedActiveReservation(
            final long carId, final long riderId, final LocalDate startDay, final LocalDate endDay) {
        final Instant start = startDay.atStartOfDay(AppTimezone.WALL_ZONE).toInstant();
        final Instant end = endDay.atStartOfDay(AppTimezone.WALL_ZONE).toInstant();
        jdbcTemplate.update(
                "INSERT INTO reservations (rider_id, car_id, start_date, end_date, status, total_price, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, 'accepted', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                riderId, carId, java.sql.Timestamp.from(start), java.sql.Timestamp.from(end));
    }
}
