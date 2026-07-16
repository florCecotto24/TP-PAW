package ar.edu.itba.paw.services.car;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.car.CarPublishPrerequisitesMissingException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.services.user.UserService;

@ExtendWith(MockitoExtension.class)
class CarPublishingServiceImplTest {

    private static final long OWNER_ID = 42L;
    private static final long MODEL_ID = 7L;
    private static final long BRAND_ID = 3L;
    private static final long PUBLISHED_CAR_ID = 100L;

    @Mock
    private CarService carService;
    @Mock
    private CarBrandService carBrandService;
    @Mock
    private CarModelService carModelService;
    @Mock
    private UserService userService;
    @Mock
    private AdminService adminService;

    @InjectMocks
    private CarPublishingServiceImpl carPublishingService;

    private static User owner(final UserRole role) {
        return User.builder()
                .id(OWNER_ID)
                .email("o@test.com")
                .forename("O")
                .surname("O")
                .userRole(role)
                .build();
    }

    private static CarBrand brand(final boolean validated) {
        return CarBrand.builder().id(BRAND_ID).name("Toyota").validated(validated).build();
    }

    private static CarModel model(final boolean validated) {
        return CarModel.builder()
                .id(MODEL_ID)
                .brand(brand(true))
                .name("Corolla")
                .validated(validated)
                .type(Car.Type.SEDAN)
                .build();
    }

    private static PublishCarRequest request() {
        return PublishCarRequest.builder()
                .brand("Toyota")
                .model("Corolla")
                .type(Car.Type.SEDAN)
                .plate("AA123BB")
                .year(2020)
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .description("desc")
                .build();
    }

    private static Car publishedCar() {
        return Car.builder()
                .id(PUBLISHED_CAR_ID)
                .owner(owner(UserRole.USER))
                .plate("AA123BB")
                .year(2020)
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .status(Car.Status.ACTIVE)
                .build();
    }

    @Test
    void testPublishByNamePublishesWhenCatalogAlreadyValidated() {
        // 1.Arrange
        final User user = owner(UserRole.USER);
        Mockito.when(userService.getUserById(OWNER_ID)).thenReturn(Optional.of(user));
        Mockito.when(userService.meetsPublishingPrerequisites(user)).thenReturn(true);
        Mockito.when(carBrandService.findOrCreateUnvalidated("Toyota")).thenReturn(Optional.of(brand(true)));
        Mockito.when(carModelService.findOrCreateUnvalidated(BRAND_ID, "Corolla", Car.Type.SEDAN))
                .thenReturn(Optional.of(model(true)));
        Mockito.when(carService.publishCar(
                        Mockito.eq(OWNER_ID),
                        Mockito.eq("AA123BB"),
                        Mockito.eq(MODEL_ID),
                        Mockito.eq(2020),
                        Mockito.eq(Car.Powertrain.GASOLINE),
                        Mockito.eq(Car.Transmission.MANUAL),
                        Mockito.eq("desc"),
                        Mockito.anyList(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull()))
                .thenReturn(publishedCar());

        // 2.Act
        final PublishCarOutcome outcome =
                carPublishingService.publishCar(OWNER_ID, request(), Locale.ENGLISH);

        // 3.Assert
        Assertions.assertEquals(PublishCarOutcome.Kind.PUBLISHED, outcome.getKind());
        Assertions.assertEquals(PUBLISHED_CAR_ID, outcome.getCar().getId());
    }

    @Test
    void testPublishByNameReturnsPendingWhenOwnerIntroducesUnvalidatedCatalog() {
        // 1.Arrange
        final User user = owner(UserRole.USER);
        Mockito.when(userService.getUserById(OWNER_ID)).thenReturn(Optional.of(user));
        Mockito.when(userService.meetsPublishingPrerequisites(user)).thenReturn(true);
        Mockito.when(carBrandService.findOrCreateUnvalidated("Toyota")).thenReturn(Optional.of(brand(false)));
        Mockito.when(carModelService.findOrCreateUnvalidated(BRAND_ID, "Corolla", Car.Type.SEDAN))
                .thenReturn(Optional.of(model(false)));
        Mockito.when(carService.publishCar(
                        Mockito.eq(OWNER_ID),
                        Mockito.eq("AA123BB"),
                        Mockito.eq(MODEL_ID),
                        Mockito.eq(2020),
                        Mockito.eq(Car.Powertrain.GASOLINE),
                        Mockito.eq(Car.Transmission.MANUAL),
                        Mockito.eq("desc"),
                        Mockito.anyList(),
                        Mockito.isNull(),
                        Mockito.isNull(),
                        Mockito.isNull()))
                .thenReturn(publishedCar());

        // 2.Act
        final PublishCarOutcome outcome =
                carPublishingService.publishCar(OWNER_ID, request(), Locale.ENGLISH);

        // 3.Assert
        Assertions.assertEquals(PublishCarOutcome.Kind.PENDING_VALIDATION, outcome.getKind());
        Assertions.assertEquals(PUBLISHED_CAR_ID, outcome.getCar().getId());
    }

    @Test
    void testPublishRejectsWhenPublishingPrerequisitesMissing() {
        // 1.Arrange
        final User user = owner(UserRole.USER);
        Mockito.when(userService.getUserById(OWNER_ID)).thenReturn(Optional.of(user));
        Mockito.when(userService.meetsPublishingPrerequisites(user)).thenReturn(false);

        // 2.Act / 3.Assert
        Assertions.assertThrows(
                CarPublishPrerequisitesMissingException.class,
                () -> carPublishingService.publishCar(OWNER_ID, request(), Locale.ENGLISH));
    }
}
