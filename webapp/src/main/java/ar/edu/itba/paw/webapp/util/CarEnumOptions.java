package ar.edu.itba.paw.webapp.util;

import ar.edu.itba.paw.models.Car;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CarEnumOptions {

    private CarEnumOptions() {
    }

    public static String getEnumName(final String enumName) {
        final String[] parts = enumName.toLowerCase().split("_");
        final StringBuilder sb = new StringBuilder();
        for (final String p : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p, 1, p.length());
        }
        return sb.toString();
    }

    public static Map<String, String> carTypeSelectOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Car.Type t : Car.Type.values()) {
            m.put(t.name(), getEnumName(t.name()));
        }
        return m;
    }

    public static Map<String, String> powertrainSelectOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("HYBRID", "Hybrid");
        m.put("ELECTRIC", "Electric");
        m.put("DIESEL", "Diesel");
        m.put("GASOLINE", "Gasoline");
        return m;
    }

    public static Map<String, String> transmissionSelectOptions() {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("MANUAL", "Manual");
        m.put("AUTOMATIC", "Automatic");
        m.put("SEMI_AUTOMATIC", "Semi-automatic");
        return m;
    }
}
