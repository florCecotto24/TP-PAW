package ar.edu.itba.paw.services.car;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;

/**
 * Brand/model day-price market insight for browse badges and owner pricing UI.
 * Persistence reads go through {@link CarService}; aggregation is in-memory (no {@code CarDao}).
 */
public interface CarMarketInsightService {

    Optional<CarPriceMarketInsight> getPriceMarketInsightForCar(Car car, Long excludeCarId);

    Map<Long, ConsumerCarCardMarketContext> resolveConsumerPriceMarketContexts(List<CarCard> cards);
}
