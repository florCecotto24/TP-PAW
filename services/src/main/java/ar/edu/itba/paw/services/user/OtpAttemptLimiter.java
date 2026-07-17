package ar.edu.itba.paw.services.user;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.exception.user.OtpAttemptsExceededException;

/**
 * In-memory failed-attempt window for numeric OTPs (email verify / password reset).
 * Caps brute-force guessing of short codes without requiring a schema migration.
 */
@Component
public class OtpAttemptLimiter {

    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 8;
    private static final int DEFAULT_LOCKOUT_MINUTES = 15;

    private final int maxFailedAttempts;
    private final Duration lockoutWindow;
    private final ConcurrentMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public OtpAttemptLimiter(final Environment environment) {
        this(
                environment.getProperty(
                        "app.validation.otp-max-failed-attempts", Integer.class, DEFAULT_MAX_FAILED_ATTEMPTS),
                Duration.ofMinutes(Math.max(
                        1,
                        environment.getProperty(
                                "app.validation.otp-lockout-minutes", Integer.class, DEFAULT_LOCKOUT_MINUTES))));
    }

    /**
     * Test-only factory (same package). Keeps a single public constructor for Spring DI
     * (Effective Java: prefer static factories over constructor overloading).
     */
    static OtpAttemptLimiter forTests(final int maxFailedAttempts, final Duration lockoutWindow) {
        return new OtpAttemptLimiter(maxFailedAttempts, lockoutWindow);
    }

    private OtpAttemptLimiter(final int maxFailedAttempts, final Duration lockoutWindow) {
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.lockoutWindow = lockoutWindow == null || lockoutWindow.isNegative() || lockoutWindow.isZero()
                ? Duration.ofMinutes(DEFAULT_LOCKOUT_MINUTES)
                : lockoutWindow;
    }

    public void assertAllowed(final String emailKey) {
        final Window window = windows.get(normalize(emailKey));
        if (window == null) {
            return;
        }
        if (window.isExpired(Instant.now(), lockoutWindow)) {
            windows.remove(normalize(emailKey), window);
            return;
        }
        if (window.failures >= maxFailedAttempts) {
            throw new OtpAttemptsExceededException();
        }
    }

    public void recordFailure(final String emailKey) {
        final String key = normalize(emailKey);
        final Instant now = Instant.now();
        windows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.isExpired(now, lockoutWindow)) {
                return new Window(1, now);
            }
            return new Window(existing.failures + 1, existing.windowStart);
        });
    }

    public void clear(final String emailKey) {
        windows.remove(normalize(emailKey));
    }

    private static String normalize(final String emailKey) {
        return emailKey == null ? "" : emailKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static final class Window {
        private final int failures;
        private final Instant windowStart;

        private Window(final int failures, final Instant windowStart) {
            this.failures = failures;
            this.windowStart = windowStart;
        }

        private boolean isExpired(final Instant now, final Duration lockoutWindow) {
            return windowStart.plus(lockoutWindow).isBefore(now);
        }
    }
}
