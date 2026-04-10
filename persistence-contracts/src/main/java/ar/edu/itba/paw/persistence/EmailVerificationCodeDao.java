package ar.edu.itba.paw.persistence;

import java.time.Instant;

public interface EmailVerificationCodeDao {

    void deleteForUser(long userId);

    void insert(long userId, String code, Instant expiresAt, Instant createdAt);

    /**
     * @return {@code true} si existía una fila válida (no vencida) y se eliminó
     */
    boolean deleteIfValid(long userId, String code, Instant now);
}
