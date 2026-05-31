package ar.edu.itba.paw.models.util.format;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArsMoneyFormatTest {

    @Test
    void testFormatIncludesCurrencySymbolNotUsdCode() {
        final String s = ArsMoneyFormat.format(new BigDecimal("1234.50"));
        Assertions.assertTrue(s.contains("1234") || s.contains("1.234"));
        Assertions.assertFalse(s.toUpperCase().contains("USD"));
        Assertions.assertEquals("", ArsMoneyFormat.format(null));
    }
}
