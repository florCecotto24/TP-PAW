package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.PriceMarketPosition;
import ar.edu.itba.paw.persistence.car.CarDao;

import ar.edu.itba.paw.services.car.view.OwnerCarDetailViewService;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;
@ExtendWith(MockitoExtension.class)
public class CarServiceImplTest {

    @Mock
    private CarDao carDao;

    @Mock
    private ImageService imageService;

    @Mock
    private CarPictureService carPictureService;

    @Mock
    private UserService userService;

    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Mock
    private EmailService emailService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private CarSearchService carSearchService;

    @Mock
    private OwnerCarDetailViewService ownerCarDetailViewService;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    public void testCreateCar(){
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

        final Car result = carService.createCar(ownerId, plate, carModelId, year, powertrain, transmission);

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

        final Optional<Car> result = carService.getCarById(carId);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(car, result.get());
    }

    @Test
    public void testGetCarByIdWhenCarDoesNotExist(){
        final long carId = 101L;
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.empty());

        final Optional<Car> result = carService.getCarById(carId);

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
    public void testToggleCarStatusFromPausedToActiveThrowsWhenOwnerIsBlocked() {
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

        final CarValidationException thrown = Assertions.assertThrows(
                CarValidationException.class,
                () -> carService.toggleCarStatus(ownerId, carId));
        Assertions.assertEquals(
                MessageKeys.CAR_ACTIVATE_OWNER_BLOCKED,
                thrown.getMessageCode());
    }

    @Test
    public void testToggleCarStatusFromActiveToPausedSucceedsRegardlessOfBlocked() {
        // Pausing an active listing has no blocked-owner check; only ACTIVE→PAUSED.
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

        final Car.Status next = carService.toggleCarStatus(ownerId, carId);

        Assertions.assertEquals(Car.Status.PAUSED, next);
    }

    @Test
    public void testReleaseAdminCarPauseSetsLackDocWhenInsuranceMissing() {
        // markCarAsAdminPaused accepts LACK_DOC inputs, so unpausing must re-check documentation
        // before promoting the listing back to ACTIVE; otherwise the car becomes bookable without
        // the insurance file the Car.Status javadoc requires for ACTIVE.
        final long ownerId = 42L;
        final long carId = 99L;
        final User owner = User.builder()
                .id(ownerId)
                .email("o@test.com")
                .forename("O")
                .surname("O")
                .cbu("0000000000000000000000")
                .build();
        final Car adminPausedCar = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("AA000AA")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .status(Car.Status.ADMIN_PAUSED)
                .build();
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(adminPausedCar));
        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(owner));
        Mockito.when(userService.hasValidCbu(owner)).thenReturn(true);

        carService.releaseAdminCarPause(carId);

        Assertions.assertEquals(Car.Status.LACK_DOC, adminPausedCar.getStatus());
    }

    @Test
    public void testReleaseAdminCarPauseSetsLackDocWhenCbuMissing() {
        final long ownerId = 42L;
        final long carId = 99L;
        final User owner = User.builder()
                .id(ownerId)
                .email("o@test.com")
                .forename("O")
                .surname("O")
                .build();
        final Car adminPausedCar = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("AA000AA")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .status(Car.Status.ADMIN_PAUSED)
                .build();
        adminPausedCar.setInsuranceFile(new StoredFile(
                7L, owner, "policy.pdf", "application/pdf", new byte[]{1}, null));
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(adminPausedCar));
        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(owner));
        Mockito.when(userService.hasValidCbu(owner)).thenReturn(false);

        carService.releaseAdminCarPause(carId);

        Assertions.assertEquals(Car.Status.LACK_DOC, adminPausedCar.getStatus());
    }

    @Test
    public void testReleaseAdminCarPauseSetsActiveWhenDocumentationComplete() {
        final long ownerId = 42L;
        final long carId = 99L;
        final User owner = User.builder()
                .id(ownerId)
                .email("o@test.com")
                .forename("O")
                .surname("O")
                .cbu("0000000000000000000000")
                .build();
        final Car adminPausedCar = Car.builder()
                .id(carId)
                .owner(owner)
                .plate("AA000AA")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .status(Car.Status.ADMIN_PAUSED)
                .build();
        adminPausedCar.setInsuranceFile(new StoredFile(
                7L, owner, "policy.pdf", "application/pdf", new byte[]{1}, null));
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(adminPausedCar));
        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(owner));
        Mockito.when(userService.hasValidCbu(owner)).thenReturn(true);

        carService.releaseAdminCarPause(carId);

        Assertions.assertEquals(Car.Status.ACTIVE, adminPausedCar.getStatus());
    }
}
