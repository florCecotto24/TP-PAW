package ar.edu.itba.paw.models.dto.car;

import java.util.Optional;

/** Resolved market badge data for one consumer browse card. */
public final class ConsumerCarCardMarketContext {

    private final PriceMarketPosition position;
    private final CarPriceMarketInsight insight;

    private ConsumerCarCardMarketContext(
            final PriceMarketPosition position,
            final CarPriceMarketInsight insight) {
        this.position = position;
        this.insight = insight;
    }

    public static Optional<ConsumerCarCardMarketContext> fromInsight(
            final Optional<CarPriceMarketInsight> insight,
            final java.math.BigDecimal dayPrice) {
        return insight.flatMap(i -> i.classifyDayPrice(dayPrice)
                .map(pos -> new ConsumerCarCardMarketContext(pos, i)));
    }

    public PriceMarketPosition getPosition() {
        return position;
    }

    public CarPriceMarketInsight getInsight() {
        return insight;
    }
}
