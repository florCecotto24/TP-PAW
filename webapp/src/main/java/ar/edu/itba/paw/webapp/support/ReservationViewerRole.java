package ar.edu.itba.paw.webapp.support;

/**
 * Type-safe replacement for the {@code role=owner|rider} query parameter that the rider/owner
 * reservation handlers expose. Binding is handled by the generic
 * {@link ar.edu.itba.paw.webapp.support.converter.StringToEnumConverterFactory}: it normalises
 * any casing of the token and rejects unknown values with HTTP 400, removing the previous
 * {@code if (!"owner".equals(role) && !"rider".equals(role))} duplication in controllers.
 *
 * The service layer still accepts the historical lowercase strings, so
 * {@link #toLegacyString()} keeps the contract surface unchanged while the web layer uses the
 * enum.
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
}
