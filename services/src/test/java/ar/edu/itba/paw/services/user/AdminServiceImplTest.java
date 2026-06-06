package ar.edu.itba.paw.services.user;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException;
import ar.edu.itba.paw.models.domain.User;

import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
                reservationMessageService,
                emailService,
                passwordEncoder);
    }

    @Test
    public void testCreateAdminUserReturnsCreatedAdminUser() {
        // Happy-path coverage of the createAdminUser facade. The "sends invitation" side-effect
        // is not asserted (would require Mockito.verify, forbidden by the test-style rules);
        // the test only confirms the call returns the User produced by the underlying service.
        // 1. Arrange
        final User created = User.builder()
                .id(99L)
                .email("newadmin@test.com")
                .forename("New")
                .surname("Admin")
                .userRole("ADMIN")
                .roleAssignedBy(10L)
                .emailValidated(true)
                .build();
        Mockito.when(passwordEncoder.encode("TempPass1!")).thenReturn("HASH");
        Mockito.when(userService.createAdminUserWithEncodedPassword(
                        Mockito.eq("newadmin@test.com"),
                        Mockito.eq("New"),
                        Mockito.eq("Admin"),
                        Mockito.eq("HASH"),
                        Mockito.eq(10L)))
                .thenReturn(created);

        // 2. Execute
        final User result = adminService.createAdminUser(
                "newadmin@test.com", "New", "Admin", "TempPass1!", 10L, Locale.ENGLISH);

        // 3. Assert: returns the user produced by the service and does not throw
        Assertions.assertNotNull(result);
        Assertions.assertEquals(99L, result.getId());
        Assertions.assertEquals("ADMIN", result.getUserRole());
    }

    @Test
    public void testCreateAdminUserPropagatesValidationException() {
        // 1. Arrange
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenReturn("HASH");
        Mockito.when(userService.createAdminUserWithEncodedPassword(
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyLong()))
                .thenThrow(new AdminPromoterNotAdminException());

        // 2. Execute and 3. Assert
        Assertions.assertThrows(AdminPromoterNotAdminException.class,
                () -> adminService.createAdminUser(
                        "x@test.com", "X", "Y", "TempPass1!", 10L, Locale.ENGLISH));
    }
}
