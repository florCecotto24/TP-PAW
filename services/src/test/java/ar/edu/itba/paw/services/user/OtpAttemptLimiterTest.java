package ar.edu.itba.paw.services.user;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ar.edu.itba.paw.exception.user.OtpAttemptsExceededException;

class OtpAttemptLimiterTest {

    @Test
    void testAllowsUntilMaxFailuresThenLocks() {
        // 1.Arrange
        final OtpAttemptLimiter limiter = OtpAttemptLimiter.forTests(3, Duration.ofMinutes(15));
        final String email = "user@example.com";

        // 2.Act
        limiter.recordFailure(email);
        limiter.recordFailure(email);
        limiter.assertAllowed(email);
        limiter.recordFailure(email);

        // 3.Assert
        Assertions.assertThrows(OtpAttemptsExceededException.class, () -> limiter.assertAllowed(email));
    }

    @Test
    void testClearResetsWindow() {
        // 1.Arrange
        final OtpAttemptLimiter limiter = OtpAttemptLimiter.forTests(2, Duration.ofMinutes(15));
        final String email = "user@example.com";
        limiter.recordFailure(email);
        limiter.recordFailure(email);

        // 2.Act
        limiter.clear(email);

        // 3.Assert
        Assertions.assertDoesNotThrow(() -> limiter.assertAllowed(email));
    }
}
