package ar.edu.itba.paw.services.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.security.UserRole;

import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.services.reservation.ReservationWorkflowService;
@ExtendWith(MockitoExtension.class)
public class AdminServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private CarService carService;

    @Mock
    private CarBrandService carBrandService;

    @Mock
    private CarModelService carModelService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private ReservationWorkflowService reservationWorkflowService;

    @Mock
    private ReservationMessageService reservationMessageService;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetService passwordResetService;

    private AdminServiceImpl adminService;

    @BeforeEach
    public void setUp() {
        adminService = new AdminServiceImpl(
                userService,
                carService,
                carBrandService,
                carModelService,
                reservationService,
                reservationWorkflowService,
                reservationMessageService,
                emailService,
                passwordEncoder,
                passwordResetService);
    }

    @Test
    public void testCreateAdminUserReturnsCreatedAdminUser() {
        // 1. Arrange
        final User created = User.builder()
                .id(99L)
                .email("newadmin@test.com")
                .forename("New")
                .surname("Admin")
                .userRole(UserRole.ADMIN)
                .roleAssignedBy(10L)
                .emailValidated(true)
                .build();
        Mockito.when(userService.findRolesForUser(10L)).thenReturn(List.of(UserRole.ADMIN));
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("HASH");
        Mockito.when(userService.createAdminUserWithEncodedPassword(
                        Mockito.eq("newadmin@test.com"),
                        Mockito.eq("New"),
                        Mockito.eq("Admin"),
                        Mockito.eq("HASH"),
                        Mockito.eq(10L)))
                .thenReturn(created);

        // 2. Act
        final User result = adminService.createAdminUser(
                "newadmin@test.com", "New", "Admin", 10L, Locale.ENGLISH);

        // 3. Assert: returns the user produced by the service and does not throw
        Assertions.assertNotNull(result);
        Assertions.assertEquals(99L, result.getId());
        Assertions.assertEquals(UserRole.ADMIN, result.getUserRole());
    }

    @Test
    public void testCreateAdminUserPropagatesValidationException() {
        // 1. Arrange
        Mockito.when(userService.findRolesForUser(10L)).thenReturn(List.of(UserRole.ADMIN));
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("HASH");
        Mockito.when(userService.createAdminUserWithEncodedPassword(
                        Mockito.eq("x@test.com"),
                        Mockito.eq("X"),
                        Mockito.eq("Y"),
                        Mockito.eq("HASH"),
                        Mockito.eq(10L)))
                .thenThrow(new AdminPromoterNotAdminException());

        // 2. Act and 3. Assert
        Assertions.assertThrows(AdminPromoterNotAdminException.class,
                () -> adminService.createAdminUser(
                        "x@test.com", "X", "Y", 10L, Locale.ENGLISH));
    }

    @Test
    public void testBlockUserRejectsNonAdminCaller() {
        // 1. Arrange
        Mockito.when(userService.findRolesForUser(99L)).thenReturn(List.of(UserRole.USER));

        // 2. Act and 3. Assert
        Assertions.assertThrows(AdminPromoterNotAdminException.class,
                () -> adminService.blockUser(50L, 99L));
    }

    @Test
    public void testUnblockUserRejectsNonAdminCaller() {
        // 1. Arrange
        Mockito.when(userService.findRolesForUser(99L)).thenReturn(List.of(UserRole.USER));

        // 2. Act and 3. Assert
        Assertions.assertThrows(AdminPromoterNotAdminException.class,
                () -> adminService.unblockUser(50L, 99L));
    }

    @Test
    public void testUnblockUserDelegatesWhenCallerIsAdmin() {
        // 1. Arrange
        Mockito.when(userService.findRolesForUser(10L)).thenReturn(List.of(UserRole.ADMIN));

        // 2. Act and 3. Assert
        Assertions.assertDoesNotThrow(() -> adminService.unblockUser(50L, 10L));
    }

    @Test
    public void testRejectCarBrandClearsCarModelsBeforeDelete() {
        // 1. Arrange — brand with one model referenced by a car; clear must run before delete.
        final long brandId = 7L;
        final long modelId = 11L;
        final long carId = 21L;
        final CarBrand brand = CarBrand.builder().id(brandId).name("PendingBrand").validated(false).build();
        final CarModel model = CarModel.builder()
                .id(modelId)
                .name("PendingModel")
                .brand(brand)
                .validated(false)
                .type(Car.Type.SEDAN)
                .build();
        final Car car = Car.builder()
                .id(carId)
                .owner(User.identities(1L, "o@test.com", "O", "Wner"))
                .plate("XYZ999")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        final List<Long> clearedCarIds = new ArrayList<>();
        final List<Long> deletedModelIds = new ArrayList<>();
        final List<Long> deletedBrandIds = new ArrayList<>();

        Mockito.when(carBrandService.findById(brandId)).thenReturn(Optional.of(brand));
        Mockito.when(carModelService.findByBrandIdOrdered(brandId)).thenReturn(List.of(model));
        Mockito.when(carService.findCarsByModelId(modelId)).thenReturn(List.of(car));
        Mockito.doAnswer(inv -> {
            clearedCarIds.add(inv.getArgument(0));
            return null;
        }).when(carService).clearCarModel(carId);
        Mockito.doAnswer(inv -> {
            deletedModelIds.add(inv.getArgument(0));
            return null;
        }).when(carModelService).deleteById(modelId);
        Mockito.doAnswer(inv -> {
            deletedBrandIds.add(inv.getArgument(0));
            return null;
        }).when(carBrandService).deleteById(brandId);

        // 2. Act
        adminService.rejectCarBrand(brandId);

        // 3. Assert — cars cleared, then model and brand removed (avoids FK 500).
        Assertions.assertEquals(List.of(carId), clearedCarIds);
        Assertions.assertEquals(List.of(modelId), deletedModelIds);
        Assertions.assertEquals(List.of(brandId), deletedBrandIds);
    }

    @Test
    public void testValidateCatalogEntryAlsoValidatesPendingBrand() {
        // 1. Arrange — pending brand + pending model; approving the model must validate both.
        final long brandId = 3L;
        final long modelId = 8L;
        final CarBrand brand = CarBrand.builder().id(brandId).name("Pirulete").validated(false).build();
        final CarModel model = CarModel.builder()
                .id(modelId)
                .name("Piruloto")
                .brand(brand)
                .validated(false)
                .type(Car.Type.CONVERTIBLE)
                .build();
        final List<Long> validatedBrandIds = new ArrayList<>();
        final List<Long> validatedModelIds = new ArrayList<>();

        Mockito.when(carModelService.findById(modelId)).thenReturn(Optional.of(model));
        Mockito.when(carService.findCarsByModelId(modelId)).thenReturn(List.of());
        Mockito.doAnswer(inv -> {
            validatedBrandIds.add(inv.getArgument(0));
            brand.setValidated(true);
            return null;
        }).when(carBrandService).markAsValidated(brandId);
        Mockito.doAnswer(inv -> {
            validatedModelIds.add(inv.getArgument(0));
            model.setValidated(true);
            return null;
        }).when(carModelService).markAsValidated(modelId);

        // 2. Act
        adminService.validateCatalogEntry(modelId, Locale.ENGLISH);

        // 3. Assert
        Assertions.assertEquals(List.of(brandId), validatedBrandIds);
        Assertions.assertEquals(List.of(modelId), validatedModelIds);
        Assertions.assertTrue(brand.isValidated());
        Assertions.assertTrue(model.isValidated());
    }

    @Test
    public void testValidateCatalogEntrySkipsAlreadyValidatedBrand() {
        // 1. Arrange — brand already validated; only the model should be marked.
        final long brandId = 3L;
        final long modelId = 8L;
        final CarBrand brand = CarBrand.builder().id(brandId).name("Toyota").validated(true).build();
        final CarModel model = CarModel.builder()
                .id(modelId)
                .name("Corolla")
                .brand(brand)
                .validated(false)
                .type(Car.Type.SEDAN)
                .build();
        final List<Long> validatedModelIds = new ArrayList<>();

        Mockito.when(carModelService.findById(modelId)).thenReturn(Optional.of(model));
        Mockito.when(carService.findCarsByModelId(modelId)).thenReturn(List.of());
        Mockito.doAnswer(inv -> {
            validatedModelIds.add(inv.getArgument(0));
            model.setValidated(true);
            return null;
        }).when(carModelService).markAsValidated(modelId);

        // 2. Act
        adminService.validateCatalogEntry(modelId, Locale.ENGLISH);

        // 3. Assert — brand left alone; model validated.
        Assertions.assertTrue(brand.isValidated());
        Assertions.assertEquals(List.of(modelId), validatedModelIds);
        Assertions.assertTrue(model.isValidated());
    }
}
