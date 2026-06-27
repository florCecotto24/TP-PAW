package ar.edu.itba.paw.webapp.support;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.car.FavCarService;
import ar.edu.itba.paw.webapp.dto.VehicleCardView;

/** Maps browse {@link CarCard} rows to {@link VehicleCardView} with optional consumer price market badges. */
@Component
public class ConsumerVehicleCardViewFactory {

    private final CarService carService;
    private final FavCarService favCarService;

    @Autowired
    public ConsumerVehicleCardViewFactory(final CarService carService, final FavCarService favCarService) {
        this.carService = carService;
        this.favCarService = favCarService;
    }

    public List<VehicleCardView> toConsumerVehicleCardViews(final List<CarCard> cards) {
        return toConsumerVehicleCardViews(cards, null);
    }

    /**
     * Same as {@link #toConsumerVehicleCardViews(List)} but additionally annotates each card with
     * the favorite-button state for {@code viewerUserId}. When {@code viewerUserId} is {@code null}
     * the heart is not rendered and no extra query is issued.
     */
    public List<VehicleCardView> toConsumerVehicleCardViews(
            final List<CarCard> cards, final Long viewerUserId) {
        if (cards == null || cards.isEmpty()) {
            return List.of();
        }
        final Map<Long, ConsumerCarCardMarketContext> contexts =
                carService.resolveConsumerPriceMarketContexts(cards);
        final Set<Long> favoritedIds = resolveFavoritedIds(cards, viewerUserId);
        return cards.stream()
                .map(card -> {
                    final ConsumerCarCardMarketContext ctx = contexts.get(card.getCarId());
                    final VehicleCardView base = ctx == null
                            ? VehicleCardView.fromCarCard(card)
                            : VehicleCardView.fromCarCard(card, ctx.getPosition(), ctx.getInsight());
                    return viewerUserId == null
                            ? base
                            : base.withFavoriteState(viewerUserId, favoritedIds);
                })
                .collect(Collectors.toList());
    }

    private Set<Long> resolveFavoritedIds(final List<CarCard> cards, final Long viewerUserId) {
        if (viewerUserId == null) {
            return Set.of();
        }
        final List<Long> carIds = cards.stream().map(CarCard::getCarId).collect(Collectors.toList());
        return favCarService.filterFavoritedCarIds(viewerUserId, carIds);
    }
}
