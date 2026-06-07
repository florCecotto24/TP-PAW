package ar.edu.itba.paw.util.format;

import java.math.BigDecimal;
import java.text.NumberFormat;

import org.springframework.stereotype.Component;

import ar.edu.itba.paw.policy.MoneyFormatPolicy;

/**
 * Formats monetary amounts with a single currency, fraction precision and grouping locale resolved
 * at boot from {@link MoneyFormatPolicy} (driven by {@code app.money.*} properties). Replaces the
 * legacy {@code ArsMoneyFormat} static utility so the currency code and grouping locale are no
 * longer hardcoded inside model / service code.
 */
@Component
public final class MoneyFormat {

    private final MoneyFormatPolicy policy;

    public MoneyFormat(final MoneyFormatPolicy policy) {
        this.policy = policy;
    }

    /**
     * @param amount nullable
     * @return empty string if {@code amount} is {@code null}; otherwise the amount formatted with
     *         the configured currency, fraction precision and grouping locale
     */
    public String format(final BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        final NumberFormat nf = NumberFormat.getCurrencyInstance(policy.getFormatLocale());
        nf.setCurrency(policy.getCurrency());
        nf.setMinimumFractionDigits(policy.getMinFractionDigits());
        nf.setMaximumFractionDigits(policy.getMaxFractionDigits());
        return nf.format(amount);
    }

    /**
     * Exposes the configured currency code so views/JS can read the same value the formatter uses
     * (e.g. {@code price-market-insight.js}'s {@code Intl.NumberFormat}).
     */
    public String getCurrencyCode() {
        return policy.getCurrencyCode();
    }
}
