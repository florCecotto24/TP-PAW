package ar.edu.itba.paw.models.email.user;

import java.util.Locale;
import java.util.Objects;

/**
 * Forgot-password reset code email.
 * Use {@link #builder()} to construct instances.
 */
public final class PasswordResetCodeEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String code;

    private PasswordResetCodeEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.code = Objects.requireNonNull(builder.code, "code");
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

    public String getCode() {
        return code;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String code;

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

        public Builder code(final String value) {
            this.code = value;
            return this;
        }

        public PasswordResetCodeEmailPayload build() {
            return new PasswordResetCodeEmailPayload(this);
        }
    }
}
