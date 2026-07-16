package ar.edu.itba.paw.webapp.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.reservation.ReservationWorkflowService;
import ar.edu.itba.paw.services.user.UserService;
import ar.edu.itba.paw.webapp.controller.car.CarController;
import ar.edu.itba.paw.webapp.controller.catalog.BrandController;
import ar.edu.itba.paw.webapp.controller.catalog.NeighborhoodController;
import ar.edu.itba.paw.webapp.controller.reservation.ReservationController;
import ar.edu.itba.paw.webapp.controller.reservation.ReservationMessageController;
import ar.edu.itba.paw.webapp.controller.user.UserController;
import ar.edu.itba.paw.webapp.security.jwt.TokenService;
import ar.edu.itba.paw.webapp.support.CurrentUserResolver;

/**
 * Boots the full web application context on embedded HSQLDB (no Postgres / Flyway) and asserts the
 * REST + stateless-JWT-security graph instantiates.
 */
@SpringJUnitWebConfig(WebContextSmokeConfig.class)
@TestPropertySource(properties = {
        "spring.profiles.active=test",
        "mail.server.password=smoke-test-not-used",
        "app.security.jwt.secret=smoke-test-jwt-secret-at-least-32-bytes-long"
})
class WebApplicationContextSmokeTest {

    @Test
    void testContextBootsAndRestSecurityGraphWires(final ApplicationContext context) {
        // 1.Arrange — Spring injects the booted context

        // 2.Act — resolve the REST + JWT security graph from the context
        final SecurityFilterChain securityFilterChain = context.getBean(SecurityFilterChain.class);
        final TokenService tokenService = context.getBean(TokenService.class);
        final CurrentUserResolver currentUserResolver = context.getBean(CurrentUserResolver.class);
        final UserController userController = context.getBean(UserController.class);
        final CarController carController = context.getBean(CarController.class);
        final ReservationController reservationController = context.getBean(ReservationController.class);
        final BrandController brandController = context.getBean(BrandController.class);
        final NeighborhoodController neighborhoodController = context.getBean(NeighborhoodController.class);
        final CarService carService = context.getBean(CarService.class);
        final UserService userService = context.getBean(UserService.class);
        final ReservationWorkflowService reservationWorkflowService =
                context.getBean(ReservationWorkflowService.class);
        final String[] messageControllerBeans =
                context.getBeanNamesForType(ReservationMessageController.class);

        // 3.Assert
        assertNotNull(context);
        assertNotNull(securityFilterChain, "SecurityFilterChain must wire");
        assertNotNull(tokenService, "JWT TokenService must wire");
        assertNotNull(currentUserResolver, "CurrentUserResolver must wire");
        assertNotNull(userController);
        assertNotNull(carController);
        assertNotNull(reservationController);
        assertNotNull(brandController);
        assertNotNull(neighborhoodController);
        assertNotNull(carService);
        assertNotNull(userService);
        assertNotNull(reservationWorkflowService);
        assertTrue(messageControllerBeans.length >= 1, "sub-resources must wire too");
    }
}
