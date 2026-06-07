package ar.edu.itba.paw.models.email.user;

import java.util.Locale;
import java.util.Objects;

/**
 * One-time plain password for a legacy user migrated to local authentication.
 * Use {@link #builder()} to construct instances.
 */
public final class MigratedUserPasswordEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String plainPassword;

    private MigratedUserPasswordEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.plainPassword = Objects.requireNonNull(builder.plainPassword, "plainPassword");
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

    public String getPlainPassword() {
        return plainPassword;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String plainPassword;

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

        public Builder plainPassword(final String value) {
            this.plainPassword = value;
            return this;
        }

        public MigratedUserPasswordEmailPayload build() {
            return new MigratedUserPasswordEmailPayload(this);
        }
    }
}
