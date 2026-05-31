package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.FavoriteValidationException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.persistence.FavCarDao;

@ExtendWith(MockitoExtension.class)
class FavCarServiceImplTest {

    private static final long CAR_ID = 7L;
    private static final long OWNER_ID = 11L;
    private static final long OTHER_USER_ID = 22L;

    @Mock
    private FavCarDao favCarDao;

    @Mock
    private CarService carService;

    private FavCarServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FavCarServiceImpl(favCarDao, carService);
    }

    private Car carOwnedBy(final long ownerUserId) {
        final User owner = User.identities(ownerUserId, "owner@test.com", "Owner", "User");
        return Car.builder()
                .id(CAR_ID)
                .owner(owner)
                .plate("AAA000")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
    }

    @Test
    void testIsFavoritedReturnsTrueWhenDaoReportsFavorited() {
        // 1. Arrange
        Mockito.when(favCarDao.isFavorited(CAR_ID, OTHER_USER_ID)).thenReturn(true);

        // 2. Act
        final boolean result = service.isFavorited(CAR_ID, OTHER_USER_ID);

        // 3. Assert
        Assertions.assertTrue(result);
    }

    @Test
    void testIsFavoritedReturnsFalseWhenDaoReportsNotFavorited() {
        // 1. Arrange
        Mockito.when(favCarDao.isFavorited(CAR_ID, OTHER_USER_ID)).thenReturn(false);

        // 2. Act
        final boolean result = service.isFavorited(CAR_ID, OTHER_USER_ID);

        // 3. Assert
        Assertions.assertFalse(result);
    }

    @Test
    void testToggleFavoriteThrowsNotFoundWhenCarDoesNotExist() {
        // 1. Arrange
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.empty());

        // 2. Act + 3. Assert
        final FavoriteValidationException ex = Assertions.assertThrows(
                FavoriteValidationException.class,
                () -> service.toggleFavorite(CAR_ID, OTHER_USER_ID));
        Assertions.assertEquals(MessageKeys.FAV_CAR_NOT_FOUND, ex.getMessageCode());
    }

    @Test
    void testToggleFavoriteThrowsCannotFavOwnWhenUserOwnsCar() {
        // 1. Arrange
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.of(carOwnedBy(OWNER_ID)));

        // 2. Act + 3. Assert
        final FavoriteValidationException ex = Assertions.assertThrows(
                FavoriteValidationException.class,
                () -> service.toggleFavorite(CAR_ID, OWNER_ID));
        Assertions.assertEquals(MessageKeys.FAV_CAR_CANNOT_FAV_OWN, ex.getMessageCode());
    }

    @Test
    void testToggleFavoriteReturnsFalseWhenCarWasAlreadyFavorited() {
        // 1. Arrange
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.of(carOwnedBy(OWNER_ID)));
        Mockito.when(favCarDao.isFavorited(CAR_ID, OTHER_USER_ID)).thenReturn(true);

        // 2. Act
        final boolean newState = service.toggleFavorite(CAR_ID, OTHER_USER_ID);

        // 3. Assert
        Assertions.assertFalse(newState);
    }

    @Test
    void testToggleFavoriteReturnsTrueWhenCarWasNotPreviouslyFavorited() {
        // 1. Arrange
        Mockito.when(carService.getCarById(CAR_ID)).thenReturn(Optional.of(carOwnedBy(OWNER_ID)));
        Mockito.when(favCarDao.isFavorited(CAR_ID, OTHER_USER_ID)).thenReturn(false);

        // 2. Act
        final boolean newState = service.toggleFavorite(CAR_ID, OTHER_USER_ID);

        // 3. Assert
        Assertions.assertTrue(newState);
    }

    @Test
    void testFindMyFavoritesReturnsPageWithDaoContentTotalAndRequestedCoordinates() {
        // 1. Arrange
        final CarCard card = CarCard.builder()
                .carId(CAR_ID)
                .brand("Toyota")
                .model("Etios")
                .status(Car.Status.ACTIVE)
                .build();
        Mockito.when(favCarDao.countFavoriteCars(Mockito.anyLong(), Mockito.anyCollection()))
                .thenReturn(1L);
        Mockito.when(favCarDao.findFavoriteCarCardsWindow(
                Mockito.anyLong(), Mockito.anyCollection(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(List.of(card));

        // 2. Act
        final Page<CarCard> page = service.findMyFavorites(OTHER_USER_ID, 0, 10);

        // 3. Assert
        Assertions.assertEquals(List.of(card), page.getContent());
        Assertions.assertEquals(1L, page.getTotalItems());
        Assertions.assertEquals(0, page.getCurrentPage());
        Assertions.assertEquals(10, page.getPageSize());
    }

    @Test
    void testFindMyFavoritesClampsNegativePageToZero() {
        // 1. Arrange
        Mockito.when(favCarDao.countFavoriteCars(Mockito.anyLong(), Mockito.anyCollection()))
                .thenReturn(0L);
        Mockito.when(favCarDao.findFavoriteCarCardsWindow(
                Mockito.anyLong(), Mockito.anyCollection(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(List.of());

        // 2. Act
        final Page<CarCard> page = service.findMyFavorites(OTHER_USER_ID, -3, 10);

        // 3. Assert
        Assertions.assertEquals(0, page.getCurrentPage());
        Assertions.assertEquals(10, page.getPageSize());
    }

    @Test
    void testFindMyFavoritesClampsNonPositivePageSizeToOne() {
        // 1. Arrange
        Mockito.when(favCarDao.countFavoriteCars(Mockito.anyLong(), Mockito.anyCollection()))
                .thenReturn(0L);
        Mockito.when(favCarDao.findFavoriteCarCardsWindow(
                Mockito.anyLong(), Mockito.anyCollection(), Mockito.anyInt(), Mockito.anyInt()))
                .thenReturn(List.of());

        // 2. Act
        final Page<CarCard> page = service.findMyFavorites(OTHER_USER_ID, 0, 0);

        // 3. Assert
        Assertions.assertEquals(0, page.getCurrentPage());
        Assertions.assertEquals(1, page.getPageSize());
    }

    @Test
    void testFilterFavoritedCarIdsReturnsDaoResult() {
        // 1. Arrange
        final List<Long> requestedIds = List.of(1L, 2L, 3L);
        final Set<Long> favoritedIds = Set.of(1L, 3L);
        Mockito.when(favCarDao.filterFavoritedCarIds(OTHER_USER_ID, requestedIds))
                .thenReturn(favoritedIds);

        // 2. Act
        final Set<Long> result = service.filterFavoritedCarIds(OTHER_USER_ID, requestedIds);

        // 3. Assert
        Assertions.assertEquals(favoritedIds, result);
    }
}
