package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.util.CarSearchCriteria;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.PriceMarketPosition;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

@ExtendWith(MockitoExtension.class)
public class CarServiceImplTest {


    @Mock
    private CarDao carDao;

    @Mock
    private ReservationTimingPolicy reservationTimingPolicy;

    @Mock
    private PaginationPolicy paginationPolicy;

    @Mock
    private UserService userService;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    public void testCreateCar(){
        // 1. Arrange
        final long carId = 1L;
        final long ownerId = 2L;
        final long carModelId = 10L;
        final String plate = "testPlate";
        final Integer year = 2020;
        final Car.Powertrain powertrain = Car.Powertrain.GASOLINE;
        final Car.Transmission transmission = Car.Transmission.MANUAL;
        final Car car = Car.builder()
                .id(carId)
                .owner(User.identities(ownerId, "o@test.com", "O", "O"))
                .plate(plate)
                .year(year)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
        Mockito.when(carDao.createCar(ownerId, plate, carModelId, year, powertrain, transmission)).thenReturn(car);

        // 2. Act
        final Car result = carService.createCar(ownerId, plate, carModelId, year, powertrain, transmission);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(car, result);
        Assertions.assertEquals(carId, result.getId());
        Assertions.assertEquals(ownerId, result.getOwnerId());
        Assertions.assertEquals(plate, result.getPlate());
        Assertions.assertEquals(Optional.of(year), result.getYear());
        Assertions.assertEquals(powertrain, result.getPowertrain());
        Assertions.assertEquals(transmission, result.getTransmission());
    }

    @Test
    public void testGetCarByIdWhenCarExists(){
        // 1. Arrange
        final long carId = 100L;
        final long ownerId = 200L;
        final String plate = "plateTest";
        final Car.Powertrain powertrain = Car.Powertrain.DIESEL;
        final Car.Transmission transmission = Car.Transmission.AUTOMATIC;
        final Car car = Car.builder()
                .id(carId)
                .owner(User.identities(ownerId, "o@test.com", "O", "O"))
                .plate(plate)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(car));

        // 2. Act
        final Optional<Car> result = carService.getCarById(carId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(car, result.get());
    }

    @Test
    public void testGetCarByIdWhenCarDoesNotExist(){
        // 1. Arrange
        final long carId = 101L;
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.empty());

        // 2. Act
        final Optional<Car> result = carService.getCarById(carId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPriceMarketInsightForCarWhenCarIsNull() {
        Assertions.assertTrue(carService.getPriceMarketInsightForCar(null, null).isEmpty());
    }

    @Test
    public void testGetPriceMarketInsightForCarWhenBrandOrModelBlank() {
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();

        Assertions.assertTrue(carService.getPriceMarketInsightForCar(car, null).isEmpty());
    }

    @Test
    public void testGetPriceMarketInsightForCarReturnsInsightFromDao() {
        final CarBrand brand = CarBrand.builder().id(1L).name("Toyota").validated(true).build();
        final CarModel carModel = CarModel.builder()
                .id(10L)
                .brand(brand)
                .name("Corolla")
                .validated(true)
                .type(Car.Type.SEDAN)
                .build();
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        car.setCarModel(carModel);
        final CarPriceMarketInsight insight = new CarPriceMarketInsight(
                new BigDecimal("10000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("12500.50"),
                3L);
        Mockito.when(carDao.findActiveDayPriceMarketInsightByBrandAndModel("Toyota", "Corolla", 42L))
                .thenReturn(Optional.of(insight));

        final Optional<CarPriceMarketInsight> result = carService.getPriceMarketInsightForCar(car, 42L);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(insight, result.get());
    }

    @Test
    public void testGetPriceMarketInsightForCarWhenDaoEmpty() {
        final CarBrand brand = CarBrand.builder().id(1L).name("Ford").validated(true).build();
        final CarModel carModel = CarModel.builder()
                .id(10L)
                .brand(brand)
                .name("Ka")
                .validated(true)
                .type(Car.Type.HATCHBACK)
                .build();
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        car.setCarModel(carModel);
        Mockito.when(carDao.findActiveDayPriceMarketInsightByBrandAndModel("Ford", "Ka", null))
                .thenReturn(Optional.empty());

        Assertions.assertTrue(carService.getPriceMarketInsightForCar(car, null).isEmpty());
    }

    @Test
    public void testResolveConsumerPriceMarketContextsWhenSampleCountBelowTwo() {
        final CarCard card = CarCard.builder()
                .carId(5L)
                .brand("Toyota")
                .model("Corolla")
                .imageId(1L)
                .dayPrice(new BigDecimal("9000.00"))
                .status(Car.Status.ACTIVE)
                .build();
        final CarPriceMarketInsight insight = new CarPriceMarketInsight(
                new BigDecimal("8000.00"),
                new BigDecimal("12000.00"),
                new BigDecimal("10000.00"),
                1L);
        Mockito.when(carDao.findActiveDayPriceMarketInsightByBrandAndModel("Toyota", "Corolla", 5L))
                .thenReturn(Optional.of(insight));

        final Map<Long, ConsumerCarCardMarketContext> contexts =
                carService.resolveConsumerPriceMarketContexts(List.of(card));

        Assertions.assertTrue(contexts.isEmpty());
    }

    @Test
    public void testResolveConsumerPriceMarketContextsReturnsPositionWhenComparable() {
        final CarCard card = CarCard.builder()
                .carId(5L)
                .brand("Toyota")
                .model("Corolla")
                .imageId(1L)
                .dayPrice(new BigDecimal("9000.00"))
                .status(Car.Status.ACTIVE)
                .build();
        final CarPriceMarketInsight insight = new CarPriceMarketInsight(
                new BigDecimal("8000.00"),
                new BigDecimal("12000.00"),
                new BigDecimal("10000.00"),
                2L);
        Mockito.when(carDao.findActiveDayPriceMarketInsightByBrandAndModel("Toyota", "Corolla", 5L))
                .thenReturn(Optional.of(insight));

        final Map<Long, ConsumerCarCardMarketContext> contexts =
                carService.resolveConsumerPriceMarketContexts(List.of(card));

        Assertions.assertEquals(1, contexts.size());
        Assertions.assertEquals(
                PriceMarketPosition.BELOW_MARKET,
                contexts.get(5L).getPosition());
    }

    @Test
    public void testBuildSearchCriteriaWithFlexibleMonthSetsFlexibleSearchTrue() {
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        Mockito.lenient().when(paginationPolicy.getUiPageSize()).thenReturn(12);
        Mockito.lenient().when(paginationPolicy.getDbFetchSize()).thenReturn(24);
        final CarSearchCriteria criteria = carService.buildSearchCriteria(
                null, null, null, null, null, null, null,
                null, null, 0, null, null, null,
                true, "2026-06", null);

        Assertions.assertTrue(criteria.isFlexibleSearch());
        Assertions.assertEquals(YearMonth.of(2026, 6), criteria.getFlexibleMonth());
        Assertions.assertNull(criteria.getFlexibleDays());
    }

    @Test
    public void testBuildSearchCriteriaWithFlexibleMonthAndDaysSetsFlexibleDays() {
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        Mockito.lenient().when(paginationPolicy.getUiPageSize()).thenReturn(12);
        Mockito.lenient().when(paginationPolicy.getDbFetchSize()).thenReturn(24);
        final CarSearchCriteria criteria = carService.buildSearchCriteria(
                null, null, null, null, null, null, null,
                null, null, 0, null, null, null,
                true, "2026-08", 7);

        Assertions.assertTrue(criteria.isFlexibleSearch());
        Assertions.assertEquals(YearMonth.of(2026, 8), criteria.getFlexibleMonth());
        Assertions.assertEquals(Integer.valueOf(7), criteria.getFlexibleDays());
    }

    @Test
    public void testToggleCarStatusFromPausedToActiveThrowsWhenOwnerIsBlocked() {
        // 1. Arrange
        final long ownerId = 42L;
        final long carId = 99L;
        final User blockedOwner = User.builder()
                .id(ownerId)
                .email("o@test.com")
                .forename("O")
                .surname("O")
                .cbu("0000000000000000000000")
                .blocked(true)
                .build();
        final Car pausedCar = Car.builder()
                .id(carId)
                .owner(blockedOwner)
                .plate("AA000AA")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .status(Car.Status.PAUSED)
                .build();
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(pausedCar));
        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(blockedOwner));
        Mockito.when(userService.hasValidCbu(blockedOwner)).thenReturn(true);

        // 2. Act + 3. Assert
        final ar.edu.itba.paw.exception.car.CarValidationException thrown = Assertions.assertThrows(
                ar.edu.itba.paw.exception.car.CarValidationException.class,
                () -> carService.toggleCarStatus(ownerId, carId));
        Assertions.assertEquals(
                ar.edu.itba.paw.exception.MessageKeys.LISTING_ACTIVATE_OWNER_BLOCKED,
                thrown.getMessageCode());
        Mockito.verify(carDao, Mockito.never()).setCarStatus(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void testToggleCarStatusFromActiveToPausedSucceedsRegardlessOfBlocked() {
        // 1. Arrange — pausing an active listing has no blocked-owner check; only ACTIVE→PAUSED.
        final long ownerId = 42L;
        final long carId = 99L;
        final User blockedOwner = User.builder()
                .id(ownerId)
                .email("o@test.com")
                .forename("O")
                .surname("O")
                .blocked(true)
                .build();
        final Car activeCar = Car.builder()
                .id(carId)
                .owner(blockedOwner)
                .plate("AA000AA")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .status(Car.Status.ACTIVE)
                .build();
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(activeCar));

        // 2. Act
        final Car.Status next = carService.toggleCarStatus(ownerId, carId);

        // 3. Assert
        Assertions.assertEquals(Car.Status.PAUSED, next);
    }

    @Test
    public void testBuildSearchCriteriaWithFlexibleFalseIsNotFlexibleSearch() {
        Mockito.lenient().when(reservationTimingPolicy.getPickupLeadHours()).thenReturn(0);
        Mockito.lenient().when(paginationPolicy.getUiPageSize()).thenReturn(12);
        Mockito.lenient().when(paginationPolicy.getDbFetchSize()).thenReturn(24);
        final CarSearchCriteria criteria = carService.buildSearchCriteria(
                null, null, null, null, null, null, null,
                null, null, 0, null, null, null,
                false, "2026-06", 3);

        Assertions.assertFalse(criteria.isFlexibleSearch());
        Assertions.assertNull(criteria.getFlexibleMonth());
    }

}
