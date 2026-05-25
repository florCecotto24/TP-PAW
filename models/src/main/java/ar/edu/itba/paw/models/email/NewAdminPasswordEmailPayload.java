package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

public class NewAdminPasswordEmailPayload {

    private final String recipientEmail;
    private final String plainPassword;
    private final Locale messageLocale;

    public NewAdminPasswordEmailPayload(final String recipientEmail, final String plainPassword, final Locale messageLocale) {
        this.recipientEmail = Objects.requireNonNull(recipientEmail);
        this.plainPassword = Objects.requireNonNull(plainPassword);
        this.messageLocale = messageLocale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getPlainPassword() {
        return plainPassword;
    }

    public Locale getMessageLocale() {
        return messageLocale;
    }
}
