package ar.edu.itba.paw.services.car;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.user.UserService;

@ExtendWith(MockitoExtension.class)
class CarListingPolicyServiceTest {

    @Mock
    private UserService userService;

    private CarListingPolicyServiceImpl policyService;

    @BeforeEach
    void setUp() {
        policyService = new CarListingPolicyServiceImpl(userService);
    }

    @Test
    void testResolveOwnerListingStatusesReturnsRequestedStatusesWhenPresent() {
        // 1.Arrange
        final List<Car.Status> requested = List.of(Car.Status.PAUSED, Car.Status.DEACTIVATED);

        // 2.Act
        final List<Car.Status> resolved = policyService.resolveOwnerListingStatuses(requested, false);

        // 3.Assert
        Assertions.assertEquals(requested, resolved);
    }

    @Test
    void testResolveOwnerListingStatusesReturnsNullForSelfOrAdminWithoutFilter() {
        // 1.Arrange
        final List<Car.Status> emptyFilter = List.of();

        // 2.Act
        final List<Car.Status> resolved = policyService.resolveOwnerListingStatuses(emptyFilter, true);

        // 3.Assert
        Assertions.assertNull(resolved);
    }

    @Test
    void testResolveOwnerListingStatusesDefaultsToActiveOnlyForOtherViewers() {
        // 1.Arrange
        final List<Car.Status> noFilter = null;

        // 2.Act
        final List<Car.Status> resolved = policyService.resolveOwnerListingStatuses(noFilter, false);

        // 3.Assert
        Assertions.assertEquals(List.of(Car.Status.ACTIVE), resolved);
    }
}
