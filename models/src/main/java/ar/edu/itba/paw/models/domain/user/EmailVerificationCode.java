package ar.edu.itba.paw.models.domain.user;

import java.time.Instant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import ar.edu.itba.paw.models.domain.internal.EntityEquality;

/** One-time email verification token row (maps {@code email_verification_codes}).
 * Scalar {@code user_id} is intentional: ephemeral OTP rows are inserted/deleted by id only;
 * navigating a {@code User} association would not be used by callers. */
@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "email_verification_codes_id_seq")
    @SequenceGenerator(name = "email_verification_codes_id_seq", sequenceName = "email_verification_codes_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /* package */ EmailVerificationCode() {
    }

    public EmailVerificationCode(final long userId, final String code, final Instant expiresAt, final Instant createdAt) {
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
        if (!(o instanceof EmailVerificationCode)) {
            return false;
        }
        return EntityEquality.equalsByLongId(this, getId(), ((EmailVerificationCode) o).getId());
    }

    @Override
    public int hashCode() {
        return EntityEquality.hashByLongId(this, id);
    }
}
