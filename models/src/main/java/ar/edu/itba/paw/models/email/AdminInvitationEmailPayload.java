package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Invitation to a newly created administrator account with a one-time temporary password.
 * Use {@link #builder()} to construct instances.
 */
public final class AdminInvitationEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String recipientFullName;
    private final String plainPassword;

    private AdminInvitationEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.recipientFullName = Objects.requireNonNull(builder.recipientFullName, "recipientFullName");
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

    public String getRecipientFullName() {
        return recipientFullName;
    }

    public String getPlainPassword() {
        return plainPassword;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String recipientFullName;
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

        public Builder recipientFullName(final String value) {
            this.recipientFullName = value;
            return this;
        }

        public Builder plainPassword(final String value) {
            this.plainPassword = value;
            return this;
        }

        public AdminInvitationEmailPayload build() {
            return new AdminInvitationEmailPayload(this);
        }
    }
}
