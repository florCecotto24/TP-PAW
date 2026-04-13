package ar.edu.itba.paw.persistence;

import java.time.Instant;

public interface PasswordResetCodeDao {

    void deleteForUser(long userId);

    void insert(long userId, String code, Instant expiresAt, Instant createdAt);

    /**
     * @return {@code true} if there was a valid row (not expired) and it was deleted
     */
    boolean deleteIfValid(long userId, String code, Instant now);

    boolean hasActiveCode(long userId, Instant now);
}
