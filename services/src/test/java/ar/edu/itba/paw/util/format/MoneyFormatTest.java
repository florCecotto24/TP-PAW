package ar.edu.itba.paw.util.format;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.policy.MoneyFormatPolicy;

public class MoneyFormatTest {

    @Test
    void testFormatNullAmountReturnsEmptyString() {
        final MoneyFormat formatter = new MoneyFormat(
                MoneyFormatPolicy.fromValidatedConfiguration("ARS", "es-AR", 2, 2));

        Assertions.assertEquals("", formatter.format(null));
    }

    @Test
    void testFormatRendersConfiguredCurrencyAndGrouping() {
        final MoneyFormat formatter = new MoneyFormat(
                MoneyFormatPolicy.fromValidatedConfiguration("ARS", "es-AR", 2, 2));

        final String s = formatter.format(new BigDecimal("1234.50"));

        Assertions.assertTrue(s.contains("1234") || s.contains("1.234"),
                "expected es-AR grouping, got: " + s);
        Assertions.assertFalse(s.toUpperCase().contains("USD"));
    }

    @Test
    void testGetCurrencyCodeReturnsConfiguredCode() {
        final MoneyFormat formatter = new MoneyFormat(
                MoneyFormatPolicy.fromValidatedConfiguration("USD", "en-US", 2, 2));

        Assertions.assertEquals("USD", formatter.getCurrencyCode());
    }

    @Test
    void testFormatWithDifferentCurrencyAndLocaleHonoursPolicy() {
        final MoneyFormat formatter = new MoneyFormat(
                MoneyFormatPolicy.fromValidatedConfiguration("USD", "en-US", 2, 2));

        final String s = formatter.format(new BigDecimal("1500"));

        Assertions.assertTrue(s.contains("1,500") || s.contains("1500"),
                "expected en-US grouping, got: " + s);
    }
}
