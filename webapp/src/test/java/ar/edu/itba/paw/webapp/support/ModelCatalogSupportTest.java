package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarMarketInsightService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.user.AdminService;

@ExtendWith(MockitoExtension.class)
class ModelCatalogSupportTest {

    @Mock
    private CarBrandService carBrandService;
    @Mock
    private CarModelService carModelService;
    @Mock
    private CarService carService;
    @Mock
    private CarMarketInsightService carMarketInsightService;
    @Mock
    private AdminService adminService;

    private ModelCatalogSupport support;

    @BeforeEach
    void setUp() {
        support = new ModelCatalogSupport(
                carBrandService, carModelService, carService, carMarketInsightService, adminService);
    }

    @Test
    void testPriceInsightRejectsExcludeCarFromOtherModel() {
        // 1.Arrange
        final long modelId = 10L;
        final long excludeCarId = 55L;
        Mockito.when(carModelService.findById(modelId)).thenReturn(Optional.of(Mockito.mock(CarModel.class)));
        final Car car = Mockito.mock(Car.class);
        final CarModel otherModel = Mockito.mock(CarModel.class);
        Mockito.when(otherModel.getId()).thenReturn(99L);
        Mockito.when(car.getCarModel()).thenReturn(Optional.of(otherModel));
        Mockito.when(carService.getCarById(excludeCarId)).thenReturn(Optional.of(car));

        // 2.Act / 3.Assert
        Assertions.assertThrows(CarNotFoundException.class,
                () -> support.priceInsightResponse(modelId, excludeCarId));
    }

    @Test
    void testPriceInsightNoContentWhenExcludeCarMissing() {
        // 1.Arrange
        final long modelId = 10L;
        Mockito.when(carModelService.findById(modelId)).thenReturn(Optional.of(Mockito.mock(CarModel.class)));

        // 2.Act
        final Response response = support.priceInsightResponse(modelId, null);

        // 3.Assert
        Assertions.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }
}
