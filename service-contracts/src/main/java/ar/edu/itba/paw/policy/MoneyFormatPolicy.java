package ar.edu.itba.paw.policy;

import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Central currency / formatting locale used by {@code MoneyFormat} for all monetary amounts the
 * app shows to users (reservation totals, price breakdowns, market insights, mailers).
 * Aligned with the {@code app.money.*} keys.
 */
public final class MoneyFormatPolicy {

    private final Currency currency;
    private final Locale formatLocale;
    private final int minFractionDigits;
    private final int maxFractionDigits;

    public static MoneyFormatPolicy fromValidatedConfiguration(
            final String currencyCode,
            final String formatLocaleTag,
            final int minFractionDigits,
            final int maxFractionDigits) {
        Objects.requireNonNull(currencyCode, "app.money.currency");
        Objects.requireNonNull(formatLocaleTag, "app.money.format-locale");
        final Currency currency;
        try {
            currency = Currency.getInstance(currencyCode);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "app.money.currency must be a valid ISO 4217 code, got '" + currencyCode + "'", e);
        }
        final Locale locale = Locale.forLanguageTag(formatLocaleTag);
        if (locale.toLanguageTag().equals("und")) {
            throw new IllegalArgumentException(
                    "app.money.format-locale must be a valid IETF language tag, got '" + formatLocaleTag + "'");
        }
        if (minFractionDigits < 0) {
            throw new IllegalArgumentException(
                    "app.money.min-fraction-digits must be >= 0, got " + minFractionDigits);
        }
        if (maxFractionDigits < minFractionDigits) {
            throw new IllegalArgumentException(
                    "app.money.max-fraction-digits must be >= min-fraction-digits, got "
                            + maxFractionDigits + " < " + minFractionDigits);
        }
        return new MoneyFormatPolicy(currency, locale, minFractionDigits, maxFractionDigits);
    }

    private MoneyFormatPolicy(
            final Currency currency,
            final Locale formatLocale,
            final int minFractionDigits,
            final int maxFractionDigits) {
        this.currency = currency;
        this.formatLocale = formatLocale;
        this.minFractionDigits = minFractionDigits;
        this.maxFractionDigits = maxFractionDigits;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    public Locale getFormatLocale() {
        return formatLocale;
    }

    public int getMinFractionDigits() {
        return minFractionDigits;
    }

    public int getMaxFractionDigits() {
        return maxFractionDigits;
    }
}
