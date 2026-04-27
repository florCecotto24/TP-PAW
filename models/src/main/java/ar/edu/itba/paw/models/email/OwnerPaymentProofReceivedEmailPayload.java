package ar.edu.itba.paw.models.email;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * Notifies the listing owner that the rider uploaded a payment receipt and should review it.
 */
public final class OwnerPaymentProofReceivedEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String ownerFullName;
    private final String riderFullName;
    private final String vehicleLabel;
    private final long reservationId;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;

    public OwnerPaymentProofReceivedEmailPayload(
            final Locale messageLocale,
            final String recipientEmail,
            final String ownerFullName,
            final String riderFullName,
            final String vehicleLabel,
            final long reservationId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        this.messageLocale = Objects.requireNonNull(messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail");
        this.ownerFullName = Objects.requireNonNull(ownerFullName, "ownerFullName");
        this.riderFullName = Objects.requireNonNull(riderFullName, "riderFullName");
        this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
        this.reservationId = reservationId;
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
    }

    public Locale getMessageLocale() {
        return messageLocale;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getRiderFullName() {
        return riderFullName;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public long getReservationId() {
        return reservationId;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }
}
