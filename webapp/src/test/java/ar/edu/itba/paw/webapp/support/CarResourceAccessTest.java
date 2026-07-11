package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.reservation.ReservationService;
import ar.edu.itba.paw.webapp.security.auth.userdetails.RydenUserDetails;

@ExtendWith(MockitoExtension.class)
class CarResourceAccessTest {

    @Mock
    private CarService carService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private Car car;

    private CarResourceAccess access;
    private RydenUserDetails ownerViewer;
    private RydenUserDetails otherViewer;
    private RydenUserDetails riderViewer;

    @BeforeEach
    void setUp() {
        access = new CarResourceAccess(carService, reservationService);
        ownerViewer = viewer(10L);
        otherViewer = viewer(20L);
        riderViewer = viewer(30L);
    }

    @Test
    void testActiveCarIsPubliclyReadable() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.ACTIVE);

        // 2.Act
        final boolean publiclyReadable = access.isPubliclyReadable(car);
        final boolean anonymousCanView = access.canViewCar(car, null);

        // 3.Assert
        assertTrue(publiclyReadable);
        assertTrue(anonymousCanView);
    }

    @Test
    void testDeactivatedCarVisibleOnlyToOwner() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.DEACTIVATED);
        when(car.getOwnerId()).thenReturn(10L);
        when(car.getId()).thenReturn(5L);
        when(reservationService.existsRiderReservationForCar(20L, 5L)).thenReturn(false);

        // 2.Act
        final boolean publiclyReadable = access.isPubliclyReadable(car);
        final boolean ownerCanView = access.canViewCar(car, ownerViewer);
        final boolean otherCanView = access.canViewCar(car, otherViewer);
        final boolean anonymousCanView = access.canViewCar(car, null);

        // 3.Assert
        assertFalse(publiclyReadable);
        assertTrue(ownerCanView);
        assertFalse(otherCanView);
        assertFalse(anonymousCanView);
    }

    @Test
    void testDeactivatedCarVisibleToRiderWithReservation() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.DEACTIVATED);
        when(car.getOwnerId()).thenReturn(10L);
        when(car.getId()).thenReturn(5L);
        when(reservationService.existsRiderReservationForCar(30L, 5L)).thenReturn(true);

        // 2.Act
        final boolean riderCanView = access.canViewCar(car, riderViewer);

        // 3.Assert
        assertTrue(riderCanView);
    }

    @Test
    void testDeactivatedCarHiddenFromStrangerWithoutReservation() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.PAUSED);
        when(car.getOwnerId()).thenReturn(10L);
        when(car.getId()).thenReturn(5L);
        when(reservationService.existsRiderReservationForCar(20L, 5L)).thenReturn(false);

        // 2.Act
        final boolean strangerCanView = access.canViewCar(car, otherViewer);

        // 3.Assert
        assertFalse(strangerCanView);
    }

    @Test
    void testCanViewCarByIdRespectsOwnerVisibility() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.DEACTIVATED);
        when(car.getOwnerId()).thenReturn(10L);
        when(car.getId()).thenReturn(5L);
        when(carService.getCarById(5L)).thenReturn(Optional.of(car));
        when(reservationService.existsRiderReservationForCar(20L, 5L)).thenReturn(false);

        // 2.Act
        final boolean ownerCanView = access.canViewCarById(5L, ownerViewer);
        final boolean otherCanView = access.canViewCarById(5L, otherViewer);

        // 3.Assert
        assertTrue(ownerCanView);
        assertFalse(otherCanView);
    }

    @Test
    void testRequireViewableCarReturnsCarForOwner() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.DEACTIVATED);
        when(car.getOwnerId()).thenReturn(10L);
        when(carService.getCarById(5L)).thenReturn(Optional.of(car));

        // 2.Act
        final Car viewable = access.requireViewableCar(5L, ownerViewer);

        // 3.Assert
        assertSame(car, viewable);
    }

    @Test
    void testRequireViewableCarThrowsWhenCarMissing() {
        // 1.Arrange
        when(carService.getCarById(5L)).thenReturn(Optional.empty());

        // 2.Act / 3.Assert
        assertThrows(CarNotFoundException.class, () -> access.requireViewableCar(5L, ownerViewer));
    }

    @Test
    void testRequireViewableCarThrowsWhenViewerCannotRead() {
        // 1.Arrange
        when(car.getStatus()).thenReturn(Car.Status.DEACTIVATED);
        when(car.getOwnerId()).thenReturn(10L);
        when(car.getId()).thenReturn(5L);
        when(carService.getCarById(5L)).thenReturn(Optional.of(car));
        when(reservationService.existsRiderReservationForCar(20L, 5L)).thenReturn(false);

        // 2.Act / 3.Assert
        assertThrows(CarNotFoundException.class, () -> access.requireViewableCar(5L, otherViewer));
    }

    private static RydenUserDetails viewer(final long userId) {
        return new RydenUserDetails(userId, "u@example.com", "A", "B", "hash", List.of(), null);
    }
}
