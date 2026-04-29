package ar.edu.itba.paw.persistence.jdbc;

import ar.edu.itba.paw.persistence.EmailVerificationCodeDao;
import java.sql.Timestamp;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class EmailVerificationCodeJdbcDao implements EmailVerificationCodeDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public EmailVerificationCodeJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void deleteForUser(final long userId) {
        jdbcTemplate.update("DELETE FROM email_verification_codes WHERE user_id = ?", userId);
    }

    @Override
    public void insert(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
        jdbcTemplate.update(
                "INSERT INTO email_verification_codes (user_id, code, expires_at, created_at) VALUES (?, ?, ?, ?)",
                userId,
                code,
                Timestamp.from(expiresAt),
                Timestamp.from(createdAt));
    }

    @Override
    public boolean deleteIfValid(final long userId, final String code, final Instant now) {
        final int n = jdbcTemplate.update(
                "DELETE FROM email_verification_codes WHERE user_id = ? AND code = ? AND expires_at > ?",
                userId,
                code.trim(),
                Timestamp.from(now));
        return n > 0;
    }

    @Override
    public boolean hasActiveCode(final long userId, final Instant now) {
        final Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM email_verification_codes WHERE user_id = ? AND expires_at > ?",
                Integer.class,
                userId,
                Timestamp.from(now));
        return n != null && n > 0;
    }
}
