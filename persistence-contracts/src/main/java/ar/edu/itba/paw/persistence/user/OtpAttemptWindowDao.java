package ar.edu.itba.paw.persistence.user;

import java.time.Instant;
import java.util.Optional;

import ar.edu.itba.paw.models.domain.user.OtpAttemptWindow;

/** Failed OTP attempt windows keyed by normalized email. */
public interface OtpAttemptWindowDao {

    Optional<OtpAttemptWindow> findByEmailKey(String emailKey);

    OtpAttemptWindow save(OtpAttemptWindow window);

    void deleteByEmailKey(String emailKey);

    /** Creates or replaces the window for {@code emailKey} (dirty-check friendly upsert). */
    OtpAttemptWindow upsert(String emailKey, int failures, Instant windowStart);
}
