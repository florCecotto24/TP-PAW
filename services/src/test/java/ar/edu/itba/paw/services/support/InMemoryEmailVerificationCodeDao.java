package ar.edu.itba.paw.services.support;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.paw.persistence.user.EmailVerificationCodeDao;

/**
 * State-based in-memory fake for {@link EmailVerificationCodeDao} (AGENTS.md rule TEST-8): the
 * single active row per user lives in a map, writes mutate it and the read-side contract
 * ({@code hasActiveCode}, {@code deleteIfValid}) answers from it. Tests observe the SUT through
 * this persisted state — {@link #storedCodeFor(long)} reads the row a write left behind — never
 * through recorded interactions.
 */
public class InMemoryEmailVerificationCodeDao implements EmailVerificationCodeDao {

    private record CodeRow(String code, Instant expiresAt, Instant createdAt) { }

    private final Map<Long, CodeRow> rowsByUserId = new HashMap<>();

    /** Seeds a persisted row, as if a previous issuance had stored it. */
    public void seedCode(final long userId, final String code, final Instant expiresAt) {
        rowsByUserId.put(userId, new CodeRow(code, expiresAt, Instant.now()));
    }

    /** The code currently persisted for the user (regardless of expiry); empty when none stored. */
    public Optional<String> storedCodeFor(final long userId) {
        return Optional.ofNullable(rowsByUserId.get(userId)).map(CodeRow::code);
    }

    @Override
    public void deleteForUser(final long userId) {
        rowsByUserId.remove(userId);
    }

    @Override
    public void insert(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
        rowsByUserId.put(userId, new CodeRow(code, expiresAt, createdAt));
    }

    @Override
    public boolean deleteIfValid(final long userId, final String code, final Instant now) {
        final CodeRow row = rowsByUserId.get(userId);
        if (row == null || !row.code().equals(code) || !row.expiresAt().isAfter(now)) {
            return false;
        }
        rowsByUserId.remove(userId);
        return true;
    }

    @Override
    public boolean hasActiveCode(final long userId, final Instant now) {
        final CodeRow row = rowsByUserId.get(userId);
        return row != null && row.expiresAt().isAfter(now);
    }
}
