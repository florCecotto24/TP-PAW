package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Notifies a user that they have been promoted to administrator by another administrator.
 * Use {@link #builder()} to construct instances.
 */
public final class AdminPromotedEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String recipientFullName;
    private final String grantedByFullName;
    private final long targetUserId;

    private AdminPromotedEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.recipientFullName = Objects.requireNonNull(builder.recipientFullName, "recipientFullName");
        this.grantedByFullName = Objects.requireNonNull(builder.grantedByFullName, "grantedByFullName");
        this.targetUserId = builder.targetUserId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Locale getMessageLocale() {
        return messageLocale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRecipientFullName() {
        return recipientFullName;
    }

    public String getGrantedByFullName() {
        return grantedByFullName;
    }

    public long getTargetUserId() {
        return targetUserId;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String recipientFullName;
        private String grantedByFullName;
        private long targetUserId;

        private Builder() {
        }

        public Builder messageLocale(final Locale value) {
            this.messageLocale = value;
            return this;
        }

        public Builder recipientEmail(final String value) {
            this.recipientEmail = value;
            return this;
        }

        public Builder recipientFullName(final String value) {
            this.recipientFullName = value;
            return this;
        }

        public Builder grantedByFullName(final String value) {
            this.grantedByFullName = value;
            return this;
        }

        public Builder targetUserId(final long value) {
            this.targetUserId = value;
            return this;
        }

        public AdminPromotedEmailPayload build() {
            return new AdminPromotedEmailPayload(this);
        }
    }
}
