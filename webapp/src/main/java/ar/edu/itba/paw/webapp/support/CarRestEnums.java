package ar.edu.itba.paw.webapp.support;

import java.util.Locale;

import ar.edu.itba.paw.models.domain.car.Car;

/** Parses REST enum tokens ({@code openapi.yaml}) into domain enums. */
public final class CarRestEnums {

    private CarRestEnums() {
    }

    public static String toRestName(final Enum<?> value) {
        return value == null ? null : value.name().toLowerCase(Locale.ROOT);
    }

    public static Car.Type parseType(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Car.Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public static Car.Powertrain parsePowertrain(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Car.Powertrain.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public static Car.Transmission parseTransmission(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if ("SEMI_AUTOMATIC".equals(normalized)) {
            return Car.Transmission.SEMI_AUTOMATIC;
        }
        return Car.Transmission.valueOf(normalized);
    }

    public static Car.Status parseStatus(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Car.Status.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
