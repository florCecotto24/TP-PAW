package ar.edu.itba.paw.webapp.support;

import java.util.Optional;

/**
 * Type-safe replacement for the {@code role=owner|rider} query parameter that the rider/owner
 * reservation handlers expose. Keeping it as an enum lets controllers rely on Spring's binding for
 * the case-insensitive validation step (see {@link StringToReservationViewerRoleConverter}) instead
 * of duplicating {@code if (!"owner".equals(role) && !"rider".equals(role))} checks.
 *
 * The service layer still accepts the historical lowercase strings, so {@link #toLegacyString()}
 * keeps the contract surface unchanged while letting the web layer move to the enum.
 */
public enum ReservationViewerRole {

    OWNER("owner"),
    RIDER("rider");

    private final String legacy;

    ReservationViewerRole(final String legacy) {
        this.legacy = legacy;
    }

    /** Lowercase token used in URLs and by the service layer ({@code "owner"} / {@code "rider"}). */
    public String toLegacyString() {
        return legacy;
    }

    /**
     * Maps the legacy lowercase string back to the enum without throwing for unknown values; used by
     * the binding converter so blank or unrecognized inputs trigger Spring's standard
     * {@code typeMismatch.*} validation flow instead of {@link IllegalArgumentException}.
     */
    public static Optional<ReservationViewerRole> fromLegacyString(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final String trimmed = raw.trim();
        for (final ReservationViewerRole role : values()) {
            if (role.legacy.equalsIgnoreCase(trimmed) || role.name().equalsIgnoreCase(trimmed)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
