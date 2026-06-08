package ar.edu.itba.paw.persistence.car;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.persistence.car.FavCarDao;
import ar.edu.itba.paw.persistence.support.DaoIntegrationTestSupport;

class FavCarJpaDaoTest extends DaoIntegrationTestSupport {

    @Autowired
    private FavCarDao dao;

    @PersistenceContext
    private EntityManager em;

    private long ownerId;
    private long viewerId;
    private long modelId;

    @BeforeEach
    void seedFixture() {
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "fav-owner@test.com", "Owner", "Test");
        ownerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "fav-owner@test.com");
        jdbcTemplate.update(
                "INSERT INTO users (email, forename, surname, member_since) VALUES (?, ?, ?, CURRENT_DATE)",
                "fav-viewer@test.com", "Viewer", "Test");
        viewerId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, "fav-viewer@test.com");

        jdbcTemplate.update("INSERT INTO car_brands (name, validated) VALUES (?, ?)", "Toyota", true);
        final Long brandId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_brands WHERE name = ?", Long.class, "Toyota");
        jdbcTemplate.update(
                "INSERT INTO car_models (brand_id, name, validated, type) VALUES (?, ?, ?, ?)",
                brandId, "Etios", true, "HATCHBACK");
        modelId = jdbcTemplate.queryForObject(
                "SELECT id FROM car_models WHERE name = ?", Long.class, "Etios");
    }

    // ----- Fixture helpers (jdbc-only: never call into the DAO under test) -------------------

    private long insertCar(final String plate, final String status) {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbcTemplate.update(
                "INSERT INTO cars (owner_id, model_id, plate, powertrain, transmission, status, created_at, updated_at, minimum_rental_days) "
                + "VALUES (?, ?, ?, 'GASOLINE', 'MANUAL', ?, ?, ?, 1)",
                ownerId, modelId, plate, status, Timestamp.from(now.toInstant()),
                Timestamp.from(now.toInstant()));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cars WHERE plate = ?", Long.class, plate);
    }

    private void insertFavorite(final long carId, final long userId, final OffsetDateTime favoritedAt) {
        jdbcTemplate.update(
                "INSERT INTO fav_cars (car_id, user_id, favorited_at) VALUES (?, ?, ?)",
                carId, userId, Timestamp.from(favoritedAt.toInstant()));
    }

    private long countFavoriteRows(final long carId, final long userId) {
        final Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM fav_cars WHERE car_id = ? AND user_id = ?",
                Long.class, carId, userId);
        return count == null ? 0L : count;
    }

    // ----- Tests -----------------------------------------------------------------------------

    @Test
    void addFavorite_persistsRowInFavCarsTable() {
        // 1. Arrange — only seed the car via JdbcTemplate; fav_cars must be empty.
        final long carId = insertCar("FAV001", "active");

        // 2. Act
        dao.addFavorite(carId, viewerId, OffsetDateTime.now());
        em.flush();

        // 3. Assert — read back via JdbcTemplate, not via FavCarDao.
        Assertions.assertEquals(1L, countFavoriteRows(carId, viewerId));
        Assertions.assertEquals(0L, countFavoriteRows(carId, ownerId));
    }

    @Test
    void isFavorited_returnsTrueWhenRowExistsAndFalseOtherwise() {
        // 1. Arrange — insert one favorite directly via JdbcTemplate.
        final long carId = insertCar("FAV010", "active");
        insertFavorite(carId, viewerId, OffsetDateTime.now(ZoneOffset.UTC));

        // 2. Act
        final boolean viewerHasIt = dao.isFavorited(carId, viewerId);
        final boolean ownerHasIt = dao.isFavorited(carId, ownerId);

        // 3. Assert
        Assertions.assertTrue(viewerHasIt);
        Assertions.assertFalse(ownerHasIt);
    }

    @Test
    void removeFavorite_deletesRowFromFavCarsTable() {
        // 1. Arrange — seed both the car and the favorite via JdbcTemplate.
        final long carId = insertCar("FAV002", "active");
        insertFavorite(carId, viewerId, OffsetDateTime.now(ZoneOffset.UTC));

        // 2. Act
        dao.removeFavorite(carId, viewerId);
        em.flush();

        // 3. Assert
        Assertions.assertEquals(0L, countFavoriteRows(carId, viewerId));
    }

    @Test
    void removeFavoriteIsNoOpWhenPairAbsent() {
        // 1. Arrange — car exists but the (car, user) favorite does not.
        final long carId = insertCar("FAV020", "active");

        // 2. Act
        dao.removeFavorite(carId, viewerId);
        em.flush();

        // 3. Assert — no row was ever there, and none was inadvertently created.
        Assertions.assertEquals(0L, countFavoriteRows(carId, viewerId));
    }

    @Test
    void findFavoriteCarCardsReturnsMostRecentFirst_andRespectsStatusFilter() {
        // 1. Arrange — three favorites: one each in ACTIVE, PAUSED and DEACTIVATED. The
        // deactivated one must be filtered out; the remaining two must come back in
        // most-recently-favorited-first order.
        final long activeCarId = insertCar("FAV003", "active");
        final long pausedCarId = insertCar("FAV004", "paused");
        final long deactivatedCarId = insertCar("FAV005", "deactivated");
        final OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusHours(3);
        insertFavorite(activeCarId, viewerId, base);
        insertFavorite(pausedCarId, viewerId, base.plusHours(1));
        insertFavorite(deactivatedCarId, viewerId, base.plusHours(2));
        final Set<Car.Status> allowed = Set.of(Car.Status.ACTIVE, Car.Status.PAUSED);

        // 2. Act
        final Page<CarCard> page = dao.findFavoriteCarCards(viewerId, allowed, 0, 10);

        // 3. Assert
        final List<CarCard> content = page.getContent();
        Assertions.assertEquals(2, content.size());
        Assertions.assertEquals(2L, page.getTotalItems(),
                "DAO must compute total ignoring filtered statuses (DEACTIVATED)");
        Assertions.assertEquals(pausedCarId, content.get(0).getCarId(),
                "Most recently favorited (PAUSED) must come first");
        Assertions.assertEquals(activeCarId, content.get(1).getCarId(),
                "Earlier-favorited ACTIVE comes second");
        Assertions.assertEquals(Long.valueOf(ownerId), content.get(0).getOwnerId());
    }

    @Test
    void findFavoriteCarCardsTotalCountIgnoresFavoritesPointingToFilteredStatuses() {
        // 1. Arrange
        final long activeCarId = insertCar("FAVC01", "active");
        final long deactivatedCarId = insertCar("FAVC02", "deactivated");
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertFavorite(activeCarId, viewerId, now);
        insertFavorite(deactivatedCarId, viewerId, now);

        // 2. Act — DAO owns the total-count side of the 1+1 pagination pattern.
        final long total = dao.findFavoriteCarCards(
                viewerId, Set.of(Car.Status.ACTIVE, Car.Status.PAUSED), 0, 10).getTotalItems();

        // 3. Assert
        Assertions.assertEquals(1L, total);
    }

    @Test
    void findFavoriteCarCardsPaginatesAtSqlLevel() {
        // 1. Arrange — three favorites with ascending timestamps so the desired order is C, B, A.
        final long carA = insertCar("FAVP01", "active");
        final long carB = insertCar("FAVP02", "active");
        final long carC = insertCar("FAVP03", "active");
        final OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(30);
        insertFavorite(carA, viewerId, base);
        insertFavorite(carB, viewerId, base.plusMinutes(5));
        insertFavorite(carC, viewerId, base.plusMinutes(10));
        final Set<Car.Status> allowed = Set.of(Car.Status.ACTIVE, Car.Status.PAUSED);

        // 2. Act — page numbers (not offsets) are what the DAO API exposes now.
        final Page<CarCard> firstPage = dao.findFavoriteCarCards(viewerId, allowed, 0, 2);
        final Page<CarCard> secondPage = dao.findFavoriteCarCards(viewerId, allowed, 1, 2);

        // 3. Assert
        Assertions.assertEquals(3L, firstPage.getTotalItems());
        Assertions.assertEquals(List.of(carC, carB),
                firstPage.getContent().stream().map(CarCard::getCarId).collect(Collectors.toList()));
        Assertions.assertEquals(List.of(carA),
                secondPage.getContent().stream().map(CarCard::getCarId).collect(Collectors.toList()));
    }

    @Test
    void filterFavoritedCarIdsReturnsIntersection() {
        // 1. Arrange
        final long favoritedCar = insertCar("FAVS01", "active");
        final long otherCar = insertCar("FAVS02", "active");
        insertFavorite(favoritedCar, viewerId, OffsetDateTime.now(ZoneOffset.UTC));

        // 2. Act
        final Set<Long> result = dao.filterFavoritedCarIds(
                viewerId, List.of(favoritedCar, otherCar, 999L));

        // 3. Assert
        Assertions.assertEquals(Set.of(favoritedCar), result);
    }
}
