package ar.edu.itba.paw.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.persistence.CarDao;

@ExtendWith(MockitoExtension.class)
public class CarServiceImplTest {


    @Mock
    private CarDao carDao;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    public void testCreateCar(){
        // 1. Arrange
        final long carId = 1L;
        final long ownerId = 2L;
        final String plate = "testPlate";
        final String brand = "testBrand";
        final String model = "testModel";
        final Car.Type type = Car.Type.HATCHBACK;
        final Car.Powertrain powertrain = Car.Powertrain.GASOLINE;
        final Car.Transmission transmission = Car.Transmission.MANUAL;
        final Car car = new Car(carId, ownerId, plate, brand, model, type, powertrain, transmission);
        Mockito.when(carDao.createCar(ownerId, plate, brand, model, type, powertrain, transmission)).thenReturn(car);

        // 2. Execute
        final Car result = carService.createCar(ownerId, plate, brand, model, type, powertrain, transmission);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(car, result);
        Assertions.assertEquals(carId, result.getId());
        Assertions.assertEquals(ownerId, result.getOwnerId());
        Assertions.assertEquals(plate, result.getPlate());
        Assertions.assertEquals(brand, result.getBrand());
        Assertions.assertEquals(model, result.getModel());
        Assertions.assertEquals(type, result.getType());
        Assertions.assertEquals(powertrain, result.getPowertrain());
        Assertions.assertEquals(transmission, result.getTransmission());
    }

    @Test
    public void testGetCarByIdWhenCarExists(){
        // 1. Arrange
        final long carId = 100L;
        final long ownerId = 200L;
        final String plate = "plateTest";
        final String brand = "brandTest";
        final String model = "modelTest";
        final Car.Type type = Car.Type.SEDAN;
        final Car.Powertrain powertrain = Car.Powertrain.DIESEL;
        final Car.Transmission transmission = Car.Transmission.AUTOMATIC;
        final Car car = new Car(carId, ownerId, plate, brand, model, type, powertrain, transmission);
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(car));

        // 2. Execute
        final Optional<Car> result = carService.getCarById(carId);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(car, result.get());
    }

    @Test
    public void testGetCarByIdWhenCarDoesNotExist(){
        // 1. Arrange
        final long carId = 101L;
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.empty());

        // 2. Execute
        final Optional<Car> result = carService.getCarById(carId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetCheapestCarsWhenThereAreCars(){
        // 1. Arrange
        final Car car1 = new Car(10L, 1L, "plateTestOne", "brandTestOne", "modelTestOne", Car.Type.HATCHBACK, Car.Powertrain.GASOLINE, Car.Transmission.MANUAL);
        final Car car2 = new Car(11L, 2L, "plateTestTwo", "brandTestTwo", "modelTestTwo", Car.Type.SUV, Car.Powertrain.HYBRID, Car.Transmission.AUTOMATIC);
        final List<Car> cheapest = new ArrayList<>();
        cheapest.add(car1);
        cheapest.add(car2);
        Mockito.when(carDao.getCheapestCars()).thenReturn(cheapest);

        // 2. Execute
        final List<Car> result = carService.getCheapestCars();

        // 3. Assert
        Assertions.assertSame(cheapest, result);
    }

    @Test
    public void testGetCheapestCarsWhenThereAreNotCars(){
        // 1. Arrange
        final List<Car> empty = Collections.emptyList();
        Mockito.when(carDao.getCheapestCars()).thenReturn(empty);

        // 2. Execute
        final List<Car> result = carService.getCheapestCars();

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testGetMostRecentCarsWhenThereAreCars(){
        // 1. Arrange
        final Car car1 = new Car(20L, 3L, "plateTestOne", "brandTestOne", "modelTestOne", Car.Type.COUPE, Car.Powertrain.ELECTRIC, Car.Transmission.AUTOMATIC);
        final Car car2 = new Car(21L, 4L, "plateTestTwo", "brandTestTwo", "modelTestTwo", Car.Type.WAGON, Car.Powertrain.DIESEL, Car.Transmission.MANUAL);
        final List<Car> recent = new ArrayList<>();
        recent.add(car1);
        recent.add(car2);
        Mockito.when(carDao.getMostRecentCars()).thenReturn(recent);

        // 2. Execute
        final List<Car> result = carService.getMostRecentCars();

        // 3. Assert
        Assertions.assertSame(recent, result);
    }

    @Test
    public void testGetMostRecentCarsWhenThereAreNotCars(){
        // 1. Arrange
        final List<Car> empty = Collections.emptyList();
        Mockito.when(carDao.getMostRecentCars()).thenReturn(empty);

        // 2. Execute
        final List<Car> result = carService.getMostRecentCars();

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }

    


}
