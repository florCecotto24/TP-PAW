package ar.edu.itba.paw.webapp.support;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.services.CarService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;

/** Maps browse {@link CarCard} rows to {@link VehicleCardView} with optional consumer price market badges. */
@Component
public class ConsumerVehicleCardViewFactory {

    private final CarService carService;

    @Autowired
    public ConsumerVehicleCardViewFactory(final CarService carService) {
        this.carService = carService;
    }

    public List<VehicleCardView> toConsumerVehicleCardViews(final List<CarCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return List.of();
        }
        final Map<Long, ConsumerCarCardMarketContext> contexts =
                carService.resolveConsumerPriceMarketContexts(cards);
        return cards.stream()
                .map(card -> {
                    final ConsumerCarCardMarketContext ctx = contexts.get(card.getCarId());
                    if (ctx == null) {
                        return VehicleCardView.fromCarCard(card);
                    }
                    return VehicleCardView.fromCarCard(card, ctx.getPosition(), ctx.getInsight());
                })
                .collect(Collectors.toList());
    }
}
