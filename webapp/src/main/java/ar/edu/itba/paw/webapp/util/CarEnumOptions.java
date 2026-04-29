package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.domain.Car;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public final class CarEnumOptions {

    private static final String[] SEARCH_PRICE_BAND_KEYS = {
            "UNDER_5000", "5000_TO_15000", "15000_TO_30000", "OVER_30000"
    };

    private final MessageSource messageSource;

    public CarEnumOptions(@Qualifier("messageSource") final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    private String label(final String code, final String defaultIfMissing) {
        return messageSource.getMessage(code, null, defaultIfMissing, locale());
    }

    public Map<String, String> carTypeSelectOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Car.Type t : Car.Type.values()) {
            m.put(t.name(), label("enum.car.type." + t.name(), t.name()));
        }
        return sortByLabel(m);
    }

    public Map<String, String> powertrainSelectOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Car.Powertrain p : Car.Powertrain.values()) {
            m.put(p.name(), label("enum.car.powertrain." + p.name(), p.name()));
        }
        return sortByLabel(m);
    }

    public Map<String, String> transmissionSelectOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Car.Transmission t : Car.Transmission.values()) {
            m.put(t.name(), label("enum.car.transmission." + t.name(), t.name()));
        }
        return sortByLabel(m);
    }

    /**
     * Search price buckets in ascending order (not sorted alphabetically by label).
     */
    public Map<String, String> searchPriceBandOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final String k : SEARCH_PRICE_BAND_KEYS) {
            m.put(k, label("enum.search.price." + k, k));
        }
        return m;
    }

    private static Map<String, String> sortByLabel(final Map<String, String> unsorted) {
        return unsorted.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }
}
