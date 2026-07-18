package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.util.List;
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
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.persistence.car.CarDao;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.services.user.UserLocaleService;
import ar.edu.itba.paw.services.user.UserReadinessService;
import ar.edu.itba.paw.services.user.UserService;
@ExtendWith(MockitoExtension.class)
public class CarServiceImplTest {

    @Mock
    private CarDao carDao;

    @Mock
    private UserService userService;

    @Mock
    private UserReadinessService userReadinessService;

    @Mock
    private UserLocaleService userLocaleService;

    @Mock
    private CarAvailabilityService carAvailabilityService;

    @Mock
    private EmailService emailService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private CarSearchService carSearchService;

    @Mock
    private AdminService adminService;

    @Mock
    private CarListingPolicyService carListingPolicyService;

    @Mock
    private CarMarketInsightService carMarketInsightService;

    @Mock
    private CarGalleryMediaService carGalleryMediaService;

    @Mock
    private CarExhaustionRowProcessor carExhaustionRowProcessor;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    public void testCreateCar() {
        // 1.Arrange
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

        // 2.Act
        final Car result = carService.createCar(ownerId, plate, carModelId, year, powertrain, transmission);

        // 3.Assert
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
        // 1.Arrange
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

        // 2.Act
        final Optional<Car> result = carService.getCarById(carId);

        // 3.Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(car, result.get());
    }

    @Test
    public void testGetCarByIdWhenCarDoesNotExist(){
        // 1.Arrange
        final long carId = 101L;
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.empty());

        // 2.Act
        final Optional<Car> result = carService.getCarById(carId);

        // 3.Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPriceMarketInsightForCarDelegatesToMarketInsightService() {
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        final CarPriceMarketInsight insight = new CarPriceMarketInsight(
                new BigDecimal("10000.00"), new BigDecimal("15000.00"), new BigDecimal("12500.50"), 3L);
        Mockito.when(carMarketInsightService.getPriceMarketInsightForCar(car, 42L))
                .thenReturn(Optional.of(insight));

        Assertions.assertEquals(Optional.of(insight), carService.getPriceMarketInsightForCar(car, 42L));
    }

    @Test
    public void testResolveOwnerListingStatusesDelegatesToListingPolicyService() {
        final List<Car.Status> requested = List.of(Car.Status.PAUSED);
        Mockito.when(carListingPolicyService.resolveOwnerListingStatuses(requested, false))
                .thenReturn(requested);

        Assertions.assertEquals(requested, carService.resolveOwnerListingStatuses(requested, false));
    }

    @Test
    public void testToggleCarStatusFromPausedToActiveThrowsWhenOwnerIsBlocked() {
        // 1.Arrange
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
        Mockito.when(userReadinessService.hasValidCbu(blockedOwner)).thenReturn(true);

        // 2.Act
        final CarValidationException thrown = Assertions.assertThrows(
                CarValidationException.class,
                () -> carService.toggleCarStatus(ownerId, carId));

        // 3.Assert
        Assertions.assertEquals(
                MessageKeys.CAR_ACTIVATE_OWNER_BLOCKED,
                thrown.getMessageCode());
    }

    @Test
    public void testToggleCarStatusFromActiveToPausedSucceedsRegardlessOfBlocked() {
        // 1.Arrange
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

        // 2.Act
        final Car.Status next = carService.toggleCarStatus(ownerId, carId);

        // 3.Assert
        Assertions.assertEquals(Car.Status.PAUSED, next);
    }

    @Test
    public void testReleaseAdminCarPauseSetsLackDocWhenInsuranceMissing() {
        // 1.Arrange
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
        Mockito.when(userReadinessService.hasValidCbu(owner)).thenReturn(true);

        // 2.Act
        carService.releaseAdminCarPause(carId);

        // 3.Assert
        Assertions.assertEquals(Car.Status.LACK_DOC, adminPausedCar.getStatus());
    }

    @Test
    public void testReleaseAdminCarPauseSetsLackDocWhenCbuMissing() {
        // 1.Arrange
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
        adminPausedCar.setInsuranceFile(StoredFile.identified(
                7L, owner, "policy.pdf", "application/pdf", new byte[]{1}, null));
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(adminPausedCar));
        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(owner));
        Mockito.when(userReadinessService.hasValidCbu(owner)).thenReturn(false);

        // 2.Act
        carService.releaseAdminCarPause(carId);

        // 3.Assert
        Assertions.assertEquals(Car.Status.LACK_DOC, adminPausedCar.getStatus());
    }

    @Test
    public void testReleaseAdminCarPauseSetsActiveWhenDocumentationComplete() {
        // 1.Arrange
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
        adminPausedCar.setInsuranceFile(StoredFile.identified(
                7L, owner, "policy.pdf", "application/pdf", new byte[]{1}, null));
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(adminPausedCar));
        Mockito.when(userService.getUserById(ownerId)).thenReturn(Optional.of(owner));
        Mockito.when(userReadinessService.hasValidCbu(owner)).thenReturn(true);

        // 2.Act
        carService.releaseAdminCarPause(carId);

        // 3.Assert
        Assertions.assertEquals(Car.Status.ACTIVE, adminPausedCar.getStatus());
    }
}
