package ar.edu.itba.paw.models.email.reservation;

import java.util.Locale;
import java.util.Objects;

/**
 * Email inviting the rider to leave an optional review after the rental period.
 * Use {@link #builder()} to construct instances.
 */
public final class RiderReviewInviteEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String riderFullName;
    private final String vehicleLabel;
    private final String reviewSectionPath;

    private RiderReviewInviteEmailPayload(final Builder builder) {
        this.messageLocale = Objects.requireNonNull(builder.messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(builder.recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(builder.riderFullName, "riderFullName");
        this.vehicleLabel = Objects.requireNonNull(builder.vehicleLabel, "vehicleLabel");
        this.reviewSectionPath = Objects.requireNonNull(builder.reviewSectionPath, "reviewSectionPath");
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

    public String getRiderFullName() {
        return riderFullName;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    /**
     * Path from servlet context root for the mail CTA; must align with {@code GET /my-reservations/{id}} and fragment
     * {@code rider-review-owner} on {@code myReservationDetail.jsp}.
     */
    public String getReviewSectionPath() {
        return reviewSectionPath;
    }

    public static final class Builder {
        private Locale messageLocale;
        private String recipientEmail;
        private String riderFullName;
        private String vehicleLabel;
        private String reviewSectionPath;

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

        public Builder riderFullName(final String value) {
            this.riderFullName = value;
            return this;
        }

        public Builder vehicleLabel(final String value) {
            this.vehicleLabel = value;
            return this;
        }

        public Builder reviewSectionPath(final String value) {
            this.reviewSectionPath = value;
            return this;
        }

        public RiderReviewInviteEmailPayload build() {
            return new RiderReviewInviteEmailPayload(this);
        }
    }
}
