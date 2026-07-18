package ar.edu.itba.paw.models.domain.user;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/** Password reset token row (maps {@code password_reset_codes}).
 * Scalar {@code user_id} is intentional: ephemeral OTP rows are inserted/deleted by id only. */
@Entity
@Table(name = "password_reset_codes")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    /** SHA-256 hex digest of the mailed OTP (not the plaintext code). */
    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /* package */ PasswordResetCode() {
    }

    public PasswordResetCode(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
        this.userId = userId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getCode() {
        return code;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PasswordResetCode)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, getId(), ((PasswordResetCode) o).getId());
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }
}
