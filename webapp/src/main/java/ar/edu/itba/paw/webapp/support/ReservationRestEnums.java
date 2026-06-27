package ar.edu.itba.paw.webapp.support;

import java.util.Locale;

import ar.edu.itba.paw.models.domain.reservation.Reservation;

/** Maps reservation lifecycle enums to REST snake_case names ({@code openapi.yaml}). */
public final class ReservationRestEnums {

    private ReservationRestEnums() {
    }

    public static String toRestName(final Reservation.Status status) {
        if (status == null) {
            return null;
        }
        return status.name().toLowerCase(Locale.ROOT);
    }

    public static Reservation.Status parseStatus(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Reservation status is required.");
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return Reservation.Status.valueOf(normalized);
        } catch (final IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown reservation status: " + raw);
        }
    }
}
