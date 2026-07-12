package ar.edu.itba.paw.persistence.user;

import java.time.Instant;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.user.EmailVerificationCode;
import ar.edu.itba.paw.persistence.user.EmailVerificationCodeDao;

/**
 * OTP persistence for email verification.
 *
 * Bulk JPQL {@code DELETE} on {@code deleteForUser} / {@code deleteIfValid} is intentional:
 * codes are ephemeral credentials; atomic consume-and-delete must not race with dirty checking,
 * and replace-all-on-resend does not map to a single managed entity lifecycle.
 */
@Transactional
@Repository
public class EmailVerificationCodeJpaDao implements EmailVerificationCodeDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void deleteForUser(final long userId) {
        em.createQuery("DELETE FROM EmailVerificationCode e WHERE e.userId = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void insert(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
        em.persist(new EmailVerificationCode(userId, code, expiresAt, createdAt));
    }

    @Override
    public boolean deleteIfValid(final long userId, final String code, final Instant now) {
        final int n = em.createQuery(
                        "DELETE FROM EmailVerificationCode e "
                                + "WHERE e.userId = :userId AND e.code = :code AND e.expiresAt > :now")
                .setParameter("userId", userId)
                .setParameter("code", code.trim())
                .setParameter("now", now)
                .executeUpdate();
        return n > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveCode(final long userId, final Instant now) {
        final Number n = (Number) em.createQuery(
                        "SELECT COUNT(e) FROM EmailVerificationCode e WHERE e.userId = :userId AND e.expiresAt > :now")
                .setParameter("userId", userId)
                .setParameter("now", now)
                .getSingleResult();
        return n != null && n.longValue() > 0;
    }
}
