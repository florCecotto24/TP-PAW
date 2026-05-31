package ar.edu.itba.paw.models.util;

import java.time.ZoneId;

/**
 * Single source of truth for the application's business / wall-clock timezone.
 *
 * All Java code that needs to reason about "what day / hour is it for our users" — listing
 * availabilities, reservation windows, scheduled jobs, mail rendering, search criteria, etc. —
 * MUST read the zone from {@link #WALL_ZONE} (or {@link #ID} when a {@code String} is required,
 * such as the {@code zone} attribute of Spring's {@code @Scheduled} placeholders) instead of
 * hard-coding the IANA tag again.
 *
 * Argentina does not observe DST, so a fixed {@link ZoneId} is safe and unambiguous.
 *
 * Known duplications kept on purpose
 * The literal {@code "America/Argentina/Buenos_Aires"} still appears in two non-Java places that
 * are intentionally not centralised:
 * <b>Flyway migration {@code V26__reservations_availabilities.sql}</b> — applied migrations
 * are immutable by Flyway contract; editing them would break the checksum and lock
 * {@code flyway:migrate} in every environment that already ran it. Any future zone change
 * must ship as a new migration, not as an edit of V26.
 * <b>{@code reservation-chat.js}</b> — a client-side {@code WALL_TIMEZONE} constant.
 * Exposing this value from the server (e.g. a {@code data-attribute} rendered by JSP) would
 * add a silent failure mode (chat falls back to the browser zone) for a string that does
 * not change in practice.
 * Any future zone migration must update those two locations in coordination with this constant.
 */
public final class AppTimezone {

    public static final String ID = "America/Argentina/Buenos_Aires";
    public static final ZoneId WALL_ZONE = ZoneId.of(ID);

    private AppTimezone() {
    }
}
