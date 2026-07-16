package ar.edu.itba.paw.models.email.admin;

import java.util.Locale;
import java.util.Objects;

/**
 * Invitation to a newly created administrator account. The invitee sets their password via the
 * separate password-reset OTP email — this payload never carries a plaintext password.
 * Use {@link #builder()} to construct instances.
 */
public final class AdminInvitationEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String recipientFullName;

    private AdminInvitationEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.recipientFullName = Objects.requireNonNull(builder.recipientFullName, "recipientFullName");
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

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String recipientFullName;

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

        public AdminInvitationEmailPayload build() {
            return new AdminInvitationEmailPayload(this);
        }
    }
}
