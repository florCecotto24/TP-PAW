package ar.edu.itba.paw.webapp.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.services.car.CarPictureService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.reservation.ReservationService;

@ExtendWith(MockitoExtension.class)
class ImageResourceAccessTest {

    @Mock
    private CarPictureService carPictureService;

    @Mock
    private CarService carService;

    @Mock
    private ReservationService reservationService;

    private ImageResourceAccess imageResourceAccess;

    @BeforeEach
    void setUp() {
        final CarResourceAccess carResourceAccess =
                new CarResourceAccess(carService, reservationService);
        imageResourceAccess = new ImageResourceAccess(carPictureService, carResourceAccess);
    }

    @Test
    void testAllowsNonGalleryImage() {
        // 1.Arrange
        when(carPictureService.findCarIdsByImageId(7L)).thenReturn(List.of());

        // 2.Act / 3.Assert
        assertDoesNotThrow(() -> imageResourceAccess.requireViewableImage(7L, null));
    }

    @Test
    void testAllowsGalleryImageWhenLinkedCarIsViewable() {
        // 1.Arrange
        final Car car = org.mockito.Mockito.mock(Car.class);
        when(carPictureService.findCarIdsByImageId(7L)).thenReturn(List.of(11L));
        when(carService.getCarById(11L)).thenReturn(java.util.Optional.of(car));
        when(car.getStatus()).thenReturn(Car.Status.ACTIVE);

        // 2.Act / 3.Assert
        assertDoesNotThrow(() -> imageResourceAccess.requireViewableImage(7L, null));
    }

    @Test
    void testRejectsGalleryImageWhenNoLinkedCarIsViewable() {
        // 1.Arrange
        final Car car = org.mockito.Mockito.mock(Car.class);
        when(carPictureService.findCarIdsByImageId(7L)).thenReturn(List.of(11L));
        when(carService.getCarById(11L)).thenReturn(java.util.Optional.of(car));
        when(car.getStatus()).thenReturn(Car.Status.PAUSED);

        // 2.Act / 3.Assert
        assertThrows(
                NotFoundException.class,
                () -> imageResourceAccess.requireViewableImage(7L, null));
    }
}
