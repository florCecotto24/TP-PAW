package ar.edu.itba.paw.models.util.format;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Formats monetary amounts in Argentine pesos (ARS) for CABA / Argentina, independent of UI locale.
 */
public final class ArsMoneyFormat {

    /** Argentina locale for currency grouping and decimal separators. */
    public static final Locale LOCALE_AR = Locale.forLanguageTag("es-AR");

    private ArsMoneyFormat() {
    }

    /**
     * @param amount nullable
     * @return empty string if {@code amount} is null; otherwise ARS formatted with two fraction digits
     */
    public static String format(final BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        final NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_AR);
        nf.setCurrency(Currency.getInstance("ARS"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount);
    }
}
