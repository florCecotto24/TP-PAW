package ar.edu.itba.paw.persistence.user;

import java.time.Instant;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.user.OtpAttemptWindow;

@Transactional
@Repository
public class OtpAttemptWindowJpaDao implements OtpAttemptWindowDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public Optional<OtpAttemptWindow> findByEmailKey(final String emailKey) {
        return Optional.ofNullable(em.find(OtpAttemptWindow.class, emailKey));
    }

    @Override
    public OtpAttemptWindow save(final OtpAttemptWindow window) {
        return em.merge(window);
    }

    @Override
    public void deleteByEmailKey(final String emailKey) {
        final OtpAttemptWindow existing = em.find(OtpAttemptWindow.class, emailKey);
        if (existing != null) {
            em.remove(existing);
        }
    }

    @Override
    public OtpAttemptWindow upsert(final String emailKey, final int failures, final Instant windowStart) {
        OtpAttemptWindow window = em.find(OtpAttemptWindow.class, emailKey);
        if (window == null) {
            window = new OtpAttemptWindow(emailKey, failures, windowStart);
            em.persist(window);
            return window;
        }
        window.setFailures(failures);
        window.setWindowStart(windowStart);
        return window;
    }
}
