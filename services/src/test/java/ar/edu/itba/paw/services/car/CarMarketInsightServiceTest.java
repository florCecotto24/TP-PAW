package ar.edu.itba.paw.services.car;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarModelPriceSample;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.PriceMarketPosition;

@ExtendWith(MockitoExtension.class)
class CarMarketInsightServiceTest {

    @Mock
    private CarService carService;

    private CarMarketInsightServiceImpl insightService;

    @BeforeEach
    void setUp() {
        insightService = new CarMarketInsightServiceImpl(carService);
    }

    @Test
    void testGetPriceMarketInsightForCarWhenCarIsNull() {
        Assertions.assertTrue(insightService.getPriceMarketInsightForCar(null, null).isEmpty());
    }

    @Test
    void testGetPriceMarketInsightForCarWhenBrandOrModelBlank() {
        // 1. Arrange
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        // 2. Act
        final boolean result = insightService.getPriceMarketInsightForCar(car, null).isEmpty();
        
        // 3. Assert
        Assertions.assertTrue(result);
    }

    @Test
    void testGetPriceMarketInsightForCarReturnsInsightFromCarService() {
        // 1. Arrange
        final CarBrand brand = CarBrand.builder().id(1L).name("Toyota").validated(true).build();
        final CarModel carModel = CarModel.builder()
                .id(10L).brand(brand).name("Corolla").validated(true).type(Car.Type.SEDAN).build();
        final Car car = Car.builder()
                .id(1L)
                .owner(User.identities(2L, "o@test.com", "O", "O"))
                .plate("ABC123")
                .powertrain(Car.Powertrain.GASOLINE)
                .transmission(Car.Transmission.MANUAL)
                .build();
        car.setCarModel(carModel);
        final CarPriceMarketInsight insight = new CarPriceMarketInsight(
                new BigDecimal("10000.00"), new BigDecimal("15000.00"), new BigDecimal("12500.50"), 3L);
        Mockito.when(carService.findActiveDayPriceMarketInsightByBrandAndModel("Toyota", "Corolla", 42L))
                .thenReturn(Optional.of(insight));

        // 2. Act
        final Optional<CarPriceMarketInsight> result = insightService.getPriceMarketInsightForCar(car, 42L);

        // 3. Assert
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(insight, result.get());
    }

    @Test
    void testResolveConsumerPriceMarketContextsWhenSampleCountBelowTwo() {
        // 1. Arrange
        final CarCard card = CarCard.builder()
                .carId(5L).brand("Toyota").model("Corolla").imageId(1L)
                .dayPrice(new BigDecimal("9000.00")).status(Car.Status.ACTIVE).build();
        Mockito.when(carService.findActiveDayPricesForBrandModelPairs(List.of("Toyota"), List.of("Corolla")))
                .thenReturn(List.of(new CarModelPriceSample("Toyota", "Corolla", 6L, new BigDecimal("8000.00"))));

        // 2. Act
        final Map<Long, ConsumerCarCardMarketContext> contexts =
                insightService.resolveConsumerPriceMarketContexts(List.of(card));

        // 3. Assert
        Assertions.assertTrue(contexts.isEmpty());
    }

    @Test
    void testResolveConsumerPriceMarketContextsReturnsPositionWhenComparable() {
        // 1. Arrange
        final CarCard card = CarCard.builder()
                .carId(5L).brand("Toyota").model("Corolla").imageId(1L)
                .dayPrice(new BigDecimal("9000.00")).status(Car.Status.ACTIVE).build();
        Mockito.when(carService.findActiveDayPricesForBrandModelPairs(List.of("Toyota"), List.of("Corolla")))
                .thenReturn(List.of(
                        new CarModelPriceSample("Toyota", "Corolla", 6L, new BigDecimal("8000.00")),
                        new CarModelPriceSample("Toyota", "Corolla", 7L, new BigDecimal("12000.00"))));

        // 2. Act
        final Map<Long, ConsumerCarCardMarketContext> contexts =
                insightService.resolveConsumerPriceMarketContexts(List.of(card));

        // 3. Assert
        Assertions.assertEquals(1, contexts.size());
        Assertions.assertEquals(PriceMarketPosition.BELOW_MARKET, contexts.get(5L).getPosition());
    }

    @Test
    void testResolveConsumerPriceMarketContextsIssuesOneDaoCallForMultipleCardsSharingModel() {
        // 1. Arrange
        final CarCard cardA = CarCard.builder()
                .carId(5L).brand("Toyota").model("Corolla").imageId(1L)
                .dayPrice(new BigDecimal("9000.00")).status(Car.Status.ACTIVE).build();
        final CarCard cardB = CarCard.builder()
                .carId(6L).brand("Toyota").model("Corolla").imageId(1L)
                .dayPrice(new BigDecimal("11000.00")).status(Car.Status.ACTIVE).build();
        Mockito.when(carService.findActiveDayPricesForBrandModelPairs(List.of("Toyota"), List.of("Corolla")))
                .thenReturn(List.of(
                        new CarModelPriceSample("Toyota", "Corolla", 5L, new BigDecimal("9000.00")),
                        new CarModelPriceSample("Toyota", "Corolla", 6L, new BigDecimal("11000.00")),
                        new CarModelPriceSample("Toyota", "Corolla", 7L, new BigDecimal("10000.00"))));

        // 2. Act
        final Map<Long, ConsumerCarCardMarketContext> contexts =
                insightService.resolveConsumerPriceMarketContexts(List.of(cardA, cardB));

        // 3. Assert
        Assertions.assertEquals(2, contexts.size());
    }
}
