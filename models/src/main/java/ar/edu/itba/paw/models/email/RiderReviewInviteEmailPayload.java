package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Email inviting the rider to leave an optional review after the rental period.
 */
public final class RiderReviewInviteEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String riderFullName;
    private final String vehicleLabel;
    private final String reviewSectionPath;

    public RiderReviewInviteEmailPayload(
            final Locale messageLocale,
            final String recipientEmail,
            final String riderFullName,
            final String vehicleLabel,
            final String reviewSectionPath) {
        this.messageLocale = Objects.requireNonNull(messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(riderFullName, "riderFullName");
        this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
        this.reviewSectionPath = Objects.requireNonNull(reviewSectionPath, "reviewSectionPath");
    }

    public Locale getMessageLocale() {
        return messageLocale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRiderFullName() {
        return riderFullName;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    /** Absolute-path fragment including hash, e.g. {@code /my-reservations/12#rider-review-owner}. */
    public String getReviewSectionPath() {
        return reviewSectionPath;
    }
}
