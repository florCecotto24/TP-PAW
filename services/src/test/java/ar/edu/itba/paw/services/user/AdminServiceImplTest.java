package ar.edu.itba.paw.services.user;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException;
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
                passwordEncoder);
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
        Mockito.when(passwordEncoder.encode("TempPass1!")).thenReturn("HASH");
        Mockito.when(userService.createAdminUserWithEncodedPassword(
                        Mockito.eq("newadmin@test.com"),
                        Mockito.eq("New"),
                        Mockito.eq("Admin"),
                        Mockito.eq("HASH"),
                        Mockito.eq(10L)))
                .thenReturn(created);

        // 2. Act
        final User result = adminService.createAdminUser(
                "newadmin@test.com", "New", "Admin", "TempPass1!", 10L, Locale.ENGLISH);

        // 3. Assert: returns the user produced by the service and does not throw
        Assertions.assertNotNull(result);
        Assertions.assertEquals(99L, result.getId());
        Assertions.assertEquals(UserRole.ADMIN, result.getUserRole());
    }

    @Test
    public void testCreateAdminUserPropagatesValidationException() {
        // 1. Arrange
        Mockito.when(userService.findRolesForUser(10L)).thenReturn(List.of(UserRole.ADMIN));
        Mockito.when(passwordEncoder.encode("TempPass1!")).thenReturn("HASH");
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
                        "x@test.com", "X", "Y", "TempPass1!", 10L, Locale.ENGLISH));
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
}
