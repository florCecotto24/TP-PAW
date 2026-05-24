package ar.edu.itba.paw.services;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.User;
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
        final long carModelId = 10L;
        final String plate = "testPlate";
        final Car.Type type = Car.Type.HATCHBACK;
        final Car.Powertrain powertrain = Car.Powertrain.GASOLINE;
        final Car.Transmission transmission = Car.Transmission.MANUAL;
        final Car car = Car.builder()
                .id(carId)
                .owner(User.identities(ownerId, "o@test.com", "O", "O"))
                .plate(plate)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
        Mockito.when(carDao.createCar(ownerId, plate, carModelId, type, powertrain, transmission)).thenReturn(car);

        // 2. Act
        final Car result = carService.createCar(ownerId, plate, carModelId, type, powertrain, transmission);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(car, result);
        Assertions.assertEquals(carId, result.getId());
        Assertions.assertEquals(ownerId, result.getOwnerId());
        Assertions.assertEquals(plate, result.getPlate());
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
        final Car.Type type = Car.Type.SEDAN;
        final Car.Powertrain powertrain = Car.Powertrain.DIESEL;
        final Car.Transmission transmission = Car.Transmission.AUTOMATIC;
        final Car car = Car.builder()
                .id(carId)
                .owner(User.identities(ownerId, "o@test.com", "O", "O"))
                .plate(plate)
                .type(type)
                .powertrain(powertrain)
                .transmission(transmission)
                .build();
        Mockito.when(carDao.getCarById(carId)).thenReturn(Optional.of(car));

        // 2. Act
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

        // 2. Act
        final Optional<Car> result = carService.getCarById(carId);

        // 3. Assert
        Assertions.assertTrue(result.isEmpty());
    }



}
