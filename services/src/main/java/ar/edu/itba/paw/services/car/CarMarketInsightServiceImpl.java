package ar.edu.itba.paw.services.car;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarModelPriceSample;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;

@Service
public class CarMarketInsightServiceImpl implements CarMarketInsightService {

    private final CarService carService;

    @Autowired
    public CarMarketInsightServiceImpl(@Lazy final CarService carService) {
        this.carService = carService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarPriceMarketInsight> getPriceMarketInsightForCar(
            final Car car, final Long excludeCarId) {
        if (car == null) {
            return Optional.empty();
        }
        return normalizedBrandModel(car.getBrand(), car.getModel())
                .flatMap(bm -> carService.findActiveDayPriceMarketInsightByBrandAndModel(
                        bm[0], bm[1], excludeCarId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ConsumerCarCardMarketContext> resolveConsumerPriceMarketContexts(
            final List<CarCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return Map.of();
        }
        final Map<String, String[]> distinctPairs = new LinkedHashMap<>();
        for (final CarCard card : cards) {
            normalizedBrandModel(card.getBrand(), card.getModel())
                    .ifPresent(bm -> distinctPairs.putIfAbsent(pairKey(bm[0], bm[1]), bm));
        }
        if (distinctPairs.isEmpty()) {
            return Map.of();
        }

        final List<String> brands = new ArrayList<>(distinctPairs.size());
        final List<String> models = new ArrayList<>(distinctPairs.size());
        for (final String[] bm : distinctPairs.values()) {
            brands.add(bm[0]);
            models.add(bm[1]);
        }
        final List<CarModelPriceSample> samples =
                carService.findActiveDayPricesForBrandModelPairs(brands, models);
        final Map<String, List<CarModelPriceSample>> samplesByPair = samples.stream()
                .collect(Collectors.groupingBy(s -> pairKey(s.getBrand(), s.getModel())));

        final Map<Long, ConsumerCarCardMarketContext> contexts = new HashMap<>();
        for (final CarCard card : cards) {
            normalizedBrandModel(card.getBrand(), card.getModel())
                    .flatMap(bm -> {
                        final List<CarModelPriceSample> group =
                                samplesByPair.getOrDefault(pairKey(bm[0], bm[1]), List.of());
                        final List<BigDecimal> otherPrices = group.stream()
                                .filter(s -> s.getCarId() != card.getCarId())
                                .map(CarModelPriceSample::getMinPrice)
                                .collect(Collectors.toList());
                        return ConsumerCarCardMarketContext.fromInsight(
                                aggregate(otherPrices), card.getDayPrice());
                    })
                    .ifPresent(ctx -> contexts.put(card.getCarId(), ctx));
        }
        return contexts;
    }

    private static String pairKey(final String brand, final String model) {
        return brand.toLowerCase(Locale.ROOT) + "|" + model.toLowerCase(Locale.ROOT);
    }

    private static Optional<CarPriceMarketInsight> aggregate(final List<BigDecimal> prices) {
        if (prices.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal min = prices.get(0);
        BigDecimal max = prices.get(0);
        BigDecimal sum = BigDecimal.ZERO;
        for (final BigDecimal price : prices) {
            if (price.compareTo(min) < 0) {
                min = price;
            }
            if (price.compareTo(max) > 0) {
                max = price;
            }
            sum = sum.add(price);
        }
        final BigDecimal avg = sum.divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);
        return Optional.of(new CarPriceMarketInsight(min, max, avg, prices.size()));
    }

    private static Optional<String[]> normalizedBrandModel(final String brand, final String model) {
        if (brand == null || brand.isBlank() || model == null || model.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new String[] {brand.trim(), model.trim()});
    }
}
