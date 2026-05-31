package ar.edu.itba.paw.models.dto.car;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CarPriceMarketInsightTest {

    private static final BigDecimal AVG = new BigDecimal("10000.00");

    private static CarPriceMarketInsight insightWithSampleCount(final long sampleCount) {
        return new CarPriceMarketInsight(
                new BigDecimal("8000.00"),
                new BigDecimal("12000.00"),
                AVG,
                sampleCount);
    }

    @Test
    public void testClassifyDayPriceWhenSampleCountBelowTwo() {
        final CarPriceMarketInsight insight = insightWithSampleCount(1L);

        Assertions.assertTrue(insight.classifyDayPrice(new BigDecimal("9000.00")).isEmpty());
    }

    @Test
    public void testClassifyDayPriceWhenDayPriceNull() {
        Assertions.assertTrue(insightWithSampleCount(3L).classifyDayPrice(null).isEmpty());
    }

    @Test
    public void testClassifyDayPriceBelowMarketAtNinetyPercentThreshold() {
        final CarPriceMarketInsight insight = insightWithSampleCount(2L);

        final Optional<PriceMarketPosition> atThreshold =
                insight.classifyDayPrice(new BigDecimal("9000.00"));

        Assertions.assertEquals(Optional.of(PriceMarketPosition.BELOW_MARKET), atThreshold);
        Assertions.assertEquals(
                Optional.of(PriceMarketPosition.BELOW_MARKET),
                insight.classifyDayPrice(new BigDecimal("8999.99")));
    }

    @Test
    public void testClassifyDayPriceAtMarketBetweenThresholds() {
        final CarPriceMarketInsight insight = insightWithSampleCount(2L);

        Assertions.assertEquals(
                Optional.of(PriceMarketPosition.AT_MARKET),
                insight.classifyDayPrice(new BigDecimal("9000.01")));
        Assertions.assertEquals(
                Optional.of(PriceMarketPosition.AT_MARKET),
                insight.classifyDayPrice(new BigDecimal("11000.00")));
    }

    @Test
    public void testClassifyDayPriceAboveMarketAboveOneTenPercentThreshold() {
        final CarPriceMarketInsight insight = insightWithSampleCount(2L);

        Assertions.assertEquals(
                Optional.of(PriceMarketPosition.ABOVE_MARKET),
                insight.classifyDayPrice(new BigDecimal("11000.01")));
    }
}
