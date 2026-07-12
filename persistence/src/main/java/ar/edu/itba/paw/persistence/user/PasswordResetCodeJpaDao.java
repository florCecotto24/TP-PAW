package ar.edu.itba.paw.persistence.user;

import java.time.Instant;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.models.domain.user.PasswordResetCode;
import ar.edu.itba.paw.persistence.user.PasswordResetCodeDao;

/**
 * OTP persistence for password reset.
 *
 * Bulk JPQL {@code DELETE} on {@code deleteForUser} / {@code deleteIfValid} is intentional:
 * codes are ephemeral credentials; atomic consume-and-delete must not race with dirty checking,
 * and replace-all-on-resend does not map to a single managed entity lifecycle.
 */
@Transactional
@Repository
public class PasswordResetCodeJpaDao implements PasswordResetCodeDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void deleteForUser(final long userId) {
        em.createQuery("DELETE FROM PasswordResetCode p WHERE p.userId = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void insert(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
        em.persist(new PasswordResetCode(userId, code, expiresAt, createdAt));
    }

    @Override
    public boolean deleteIfValid(final long userId, final String code, final Instant now) {
        final int n = em.createQuery(
                        "DELETE FROM PasswordResetCode p "
                                + "WHERE p.userId = :userId AND p.code = :code AND p.expiresAt > :now")
                .setParameter("userId", userId)
                .setParameter("code", code.trim())
                .setParameter("now", now)
                .executeUpdate();
        return n > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean matchesValidCode(final long userId, final String code, final Instant now) {
        final Number n = (Number) em.createQuery(
                        "SELECT COUNT(e) FROM PasswordResetCode e "
                                + "WHERE e.userId = :userId AND e.code = :code AND e.expiresAt > :now")
                .setParameter("userId", userId)
                .setParameter("code", code.trim())
                .setParameter("now", now)
                .getSingleResult();
        return n != null && n.longValue() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveCode(final long userId, final Instant now) {
        final Number n = (Number) em.createQuery(
                        "SELECT COUNT(p) FROM PasswordResetCode p WHERE p.userId = :userId AND p.expiresAt > :now")
                .setParameter("userId", userId)
                .setParameter("now", now)
                .getSingleResult();
        return n != null && n.longValue() > 0;
    }
}
