package ar.edu.itba.paw.persistence;

import java.time.Instant;

/** Email verification codes for new accounts (single active code per user). */
public interface EmailVerificationCodeDao {

    void deleteForUser(long userId);

    void insert(long userId, String code, Instant expiresAt, Instant createdAt);

    /**
     * @return {@code true} if there was a valid row (not expired) and it was deleted
     */
    boolean deleteIfValid(long userId, String code, Instant now);

    /** There is an active code for the user. */
    boolean hasActiveCode(long userId, Instant now);
}
