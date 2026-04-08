package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
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

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.CarPicture;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.CarDao;

@ExtendWith(MockitoExtension.class)
public class CarServiceImplTest {


    @Mock
    private CarDao carDao;

    @Mock
    private UserService userService;

    @Mock
    private ListingService listingService;

    @Mock
    private ImageService imageService;

    @Mock
    private CarPictureService carPictureService;

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
    public void testPublish(){
        // 1. Arrange
        final long carId = 1L;
        final long ownerId = 2L;
        final long listingId = 3L;
        final long imageId = 4L;
        final long carPictureId = 5L;
        final String ownerEmail = "owner@test.com";
        final String ownerName = "ownerName";
        final String ownerSurname = "ownerSurname";
        final String plate = "testPlate";
        final String brand = "testBrand";
        final String model = "testModel";
        final Car.Type type = Car.Type.HATCHBACK;
        final Car.Powertrain powertrain = Car.Powertrain.GASOLINE;
        final Car.Transmission transmission = Car.Transmission.MANUAL;
        final BigDecimal pricePerDay = new BigDecimal("100");
        final String listingTitle = "listingTestTitle";
        final String startPoint = "testStartPoint";
        final String description = "testDescription";
        final OffsetDateTime createdAt = OffsetDateTime.now();
        final OffsetDateTime updatedAt = OffsetDateTime.now();
        final Listing.Status status = Listing.Status.ACTIVE;
        final LocalTime checkInTime = LocalTime.of(10, 0);
        final LocalTime checkOutTime = LocalTime.of(12, 0);
        final LocalDate startDate = LocalDate.of(2026, 1, 1);
        final LocalDate endDate = LocalDate.of(2026, 1, 31);
        final byte[] imageData = {0x00, 0x01, 0x02, 0x03};
        final String imageName = "imageNameTest";
        final String imageType = "contentTypeTest";

        
        final Image image = new Image(imageId, imageName, imageType, imageData);
        final Car car = new Car(carId, ownerId, plate, brand, model, type, powertrain, transmission);
        final Listing listing = new Listing(listingId, listingTitle, carId, createdAt, updatedAt, status, pricePerDay, startPoint, description, checkInTime, checkOutTime);
        final User user = new User(ownerId, ownerEmail, ownerName, ownerSurname);
        final CarPicture carPicture = new CarPicture(carPictureId, carId, imageId, 1, createdAt, updatedAt);
        final AvailabilityPeriod availabilityPeriod = new AvailabilityPeriod(startDate, endDate);
        final List<AvailabilityPeriod> periods = new ArrayList<>();
        periods.add(availabilityPeriod);
        final ImageUpload imageUpload = new ImageUpload(imageName, imageType, imageData);
        final List<ImageUpload> uploads = new ArrayList<>();
        uploads.add(imageUpload);

        Mockito.when(carDao.createCar(ownerId, plate, brand, model, type, powertrain, transmission)).thenReturn(car);
        Mockito.when(listingService.createListing(carId, Listing.Status.ACTIVE, pricePerDay, startPoint, description, checkInTime, checkOutTime, periods)).thenReturn(listing);
        Mockito.when(userService.findOrCreatePublisher(ownerEmail, ownerName, ownerSurname)).thenReturn(user);
        Mockito.when(imageService.createImage(imageName, imageType, imageData)).thenReturn(image);
        Mockito.when(carPictureService.createCarPicture(carId, image.getId(), 1)).thenReturn(carPicture);

        // 2. Execute
        final CarPublicationResult result = carService.publish(ownerEmail, ownerName, ownerSurname, plate, brand, model, type, powertrain, transmission, pricePerDay, startPoint, description, checkInTime, checkOutTime, periods, uploads);

        // 3. Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(user, result.getPublisher());
        Assertions.assertEquals(car, result.getCar());
        Assertions.assertEquals(listing, result.getListing());
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
