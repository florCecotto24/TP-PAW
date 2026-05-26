package ar.edu.itba.paw.models.security;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Application roles stored in {@code user_roles.role}. Add enum constants when new roles are introduced
 * in Flyway/ops; values persisted in the DB that are not listed here are ignored when building authorities
 * until they are mapped.
 */
public enum UserRole {

    /** Default authenticated user. */
    USER,

    /** Platform administrator. */
    ADMIN;

    private static final Logger LOG = LoggerFactory.getLogger(UserRole.class);

    /**
     * Exact value written to / read from {@code user_roles.role} (matches {@link #name()} for current roles).
     */
    public String persistenceName() {
        return name();
    }

    /**
     * Spring Security authority string (default {@code ROLE_*} prefix).
     */
    public String springAuthorityName() {
        return "ROLE_" + name();
    }

    /**
     * Resolves a row from {@code user_roles.role}; trimming and ASCII-uppercase matching {@link #name()}.
     */
    public static Optional<UserRole> fromPersistenceName(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UserRole.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (final IllegalArgumentException ex) {
            LOG.atDebug()
                    .setMessage("Unknown or unmapped user_roles.role value [{}]")
                    .addArgument(raw.trim())
                    .setCause(ex)
                    .log();
            return Optional.empty();
        }
    }
}
