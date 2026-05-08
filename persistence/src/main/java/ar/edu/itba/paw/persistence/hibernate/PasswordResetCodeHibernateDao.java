package ar.edu.itba.paw.persistence.hibernate;

import java.sql.Timestamp;
import java.time.Instant;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.persistence.PasswordResetCodeDao;

@Transactional
@Repository
public class PasswordResetCodeHibernateDao implements PasswordResetCodeDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void deleteForUser(final long userId) {
        em.createNativeQuery("DELETE FROM password_reset_codes WHERE user_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Override
    public void insert(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
        em.createNativeQuery(
                        "INSERT INTO password_reset_codes (user_id, code, expires_at, created_at) VALUES (:userId, :code, :expiresAt, :createdAt)")
                .setParameter("userId", userId)
                .setParameter("code", code)
                .setParameter("expiresAt", Timestamp.from(expiresAt))
                .setParameter("createdAt", Timestamp.from(createdAt))
                .executeUpdate();
    }

    @Override
    public boolean deleteIfValid(final long userId, final String code, final Instant now) {
        final int n = em.createNativeQuery(
                        "DELETE FROM password_reset_codes WHERE user_id = :userId AND code = :code AND expires_at > :now")
                .setParameter("userId", userId)
                .setParameter("code", code.trim())
                .setParameter("now", Timestamp.from(now))
                .executeUpdate();
        return n > 0;
    }

    @Override
    public boolean hasActiveCode(final long userId, final Instant now) {
        final Number n = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM password_reset_codes WHERE user_id = :userId AND expires_at > :now")
                .setParameter("userId", userId)
                .setParameter("now", Timestamp.from(now))
                .getSingleResult();
        return n != null && n.longValue() > 0;
    }
}
