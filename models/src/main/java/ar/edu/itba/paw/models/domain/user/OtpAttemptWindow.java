package ar.edu.itba.paw.models.domain.user;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Failed OTP attempt window keyed by normalized email (email verify / password reset).
 * Survives JVM restart; shared across nodes that use the same database.
 */
@Entity
@Table(name = "otp_attempt_windows")
public class OtpAttemptWindow {

    @Id
    @Column(name = "email_key", nullable = false, length = 255)
    private String emailKey;

    @Column(name = "failures", nullable = false)
    private int failures;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    /* package */ OtpAttemptWindow() {
    }

    public OtpAttemptWindow(final String emailKey, final int failures, final Instant windowStart) {
        this.emailKey = emailKey;
        this.failures = failures;
        this.windowStart = windowStart;
    }

    public String getEmailKey() {
        return emailKey;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(final int failures) {
        this.failures = failures;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(final Instant windowStart) {
        this.windowStart = windowStart;
    }
}
