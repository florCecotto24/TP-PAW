package ar.edu.itba.paw.services.user;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.user.OtpAttemptsExceededException;
import ar.edu.itba.paw.models.domain.user.OtpAttemptWindow;
import ar.edu.itba.paw.persistence.user.OtpAttemptWindowDao;

/**
 * Failed-attempt window for numeric OTPs (email verify / password reset).
 * Persisted in {@code otp_attempt_windows} so restart / multi-node share the same quota.
 * Unit tests may use {@link #forTests} (in-memory only).
 */
@Component
public class OtpAttemptLimiter {

    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 8;
    private static final int DEFAULT_LOCKOUT_MINUTES = 15;

    private final int maxFailedAttempts;
    private final Duration lockoutWindow;
    private final OtpAttemptWindowDao otpAttemptWindowDao;
    /** Non-null only for {@link #forTests} — keeps unit tests free of a persistence layer. */
    private final ConcurrentMap<String, Window> inMemoryWindows;

    @Autowired
    public OtpAttemptLimiter(final Environment environment, final OtpAttemptWindowDao otpAttemptWindowDao) {
        this(
                environment.getProperty(
                        "app.validation.otp-max-failed-attempts", Integer.class, DEFAULT_MAX_FAILED_ATTEMPTS),
                Duration.ofMinutes(Math.max(
                        1,
                        environment.getProperty(
                                "app.validation.otp-lockout-minutes", Integer.class, DEFAULT_LOCKOUT_MINUTES))),
                otpAttemptWindowDao,
                null);
    }

    /**
     * Test-only factory (same package). Keeps a single public constructor for Spring DI
     * (Effective Java: prefer static factories over constructor overloading).
     */
    static OtpAttemptLimiter forTests(final int maxFailedAttempts, final Duration lockoutWindow) {
        return new OtpAttemptLimiter(maxFailedAttempts, lockoutWindow, null, new ConcurrentHashMap<>());
    }

    private OtpAttemptLimiter(
            final int maxFailedAttempts,
            final Duration lockoutWindow,
            final OtpAttemptWindowDao otpAttemptWindowDao,
            final ConcurrentMap<String, Window> inMemoryWindows) {
        this.maxFailedAttempts = Math.max(1, maxFailedAttempts);
        this.lockoutWindow = lockoutWindow == null || lockoutWindow.isNegative() || lockoutWindow.isZero()
                ? Duration.ofMinutes(DEFAULT_LOCKOUT_MINUTES)
                : lockoutWindow;
        this.otpAttemptWindowDao = otpAttemptWindowDao;
        this.inMemoryWindows = inMemoryWindows;
    }

    @Transactional(readOnly = true)
    public void assertAllowed(final String emailKey) {
        final String key = normalize(emailKey);
        final Instant now = Instant.now();
        if (inMemoryWindows != null) {
            final Window window = inMemoryWindows.get(key);
            if (window == null) {
                return;
            }
            if (window.isExpired(now, lockoutWindow)) {
                inMemoryWindows.remove(key, window);
                return;
            }
            if (window.failures >= maxFailedAttempts) {
                throw new OtpAttemptsExceededException();
            }
            return;
        }
        final OtpAttemptWindow window = otpAttemptWindowDao.findByEmailKey(key).orElse(null);
        if (window == null) {
            return;
        }
        if (isExpired(window.getWindowStart(), now)) {
            return;
        }
        if (window.getFailures() >= maxFailedAttempts) {
            throw new OtpAttemptsExceededException();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(final String emailKey) {
        final String key = normalize(emailKey);
        final Instant now = Instant.now();
        if (inMemoryWindows != null) {
            inMemoryWindows.compute(key, (ignored, existing) -> {
                if (existing == null || existing.isExpired(now, lockoutWindow)) {
                    return new Window(1, now);
                }
                return new Window(existing.failures + 1, existing.windowStart);
            });
            return;
        }
        final OtpAttemptWindow existing = otpAttemptWindowDao.findByEmailKey(key).orElse(null);
        if (existing == null || isExpired(existing.getWindowStart(), now)) {
            otpAttemptWindowDao.upsert(key, 1, now);
            return;
        }
        otpAttemptWindowDao.upsert(key, existing.getFailures() + 1, existing.getWindowStart());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clear(final String emailKey) {
        final String key = normalize(emailKey);
        if (inMemoryWindows != null) {
            inMemoryWindows.remove(key);
            return;
        }
        otpAttemptWindowDao.deleteByEmailKey(key);
    }

    private boolean isExpired(final Instant windowStart, final Instant now) {
        return windowStart.plus(lockoutWindow).isBefore(now);
    }

    private static String normalize(final String emailKey) {
        return emailKey == null ? "" : emailKey.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /** Key space for email-verification OTP <em>issuance</em> (separate from failed-verify lockout). */
    public static String issuanceKey(final String email) {
        return "issue:" + normalize(email);
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
