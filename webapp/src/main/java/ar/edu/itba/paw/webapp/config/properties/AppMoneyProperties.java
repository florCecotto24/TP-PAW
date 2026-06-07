package ar.edu.itba.paw.webapp.config.properties;

import org.springframework.core.env.Environment;

import ar.edu.itba.paw.policy.MoneyFormatPolicy;

/**
 * Bound view of {@code app.money.*}. Add new keys here and propagate them through
 * {@link #toMoneyFormatPolicy()} so the policy factory can validate them once at boot.
 */
public record AppMoneyProperties(
        String currency,
        String formatLocale,
        int minFractionDigits,
        int maxFractionDigits) {

    private static final String CURRENCY = "app.money.currency";
    private static final String FORMAT_LOCALE = "app.money.format-locale";
    private static final String MIN_FRACTION_DIGITS = "app.money.min-fraction-digits";
    private static final String MAX_FRACTION_DIGITS = "app.money.max-fraction-digits";

    public static AppMoneyProperties fromEnvironment(final Environment environment) {
        return new AppMoneyProperties(
                environment.getProperty(CURRENCY, "ARS"),
                environment.getProperty(FORMAT_LOCALE, "es-AR"),
                environment.getProperty(MIN_FRACTION_DIGITS, Integer.class, 2),
                environment.getProperty(MAX_FRACTION_DIGITS, Integer.class, 2));
    }

    public MoneyFormatPolicy toMoneyFormatPolicy() {
        return MoneyFormatPolicy.fromValidatedConfiguration(
                currency, formatLocale, minFractionDigits, maxFractionDigits);
    }
}
