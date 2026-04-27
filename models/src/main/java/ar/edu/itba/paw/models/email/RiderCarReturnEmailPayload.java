package ar.edu.itba.paw.models.email;

import java.util.Locale;
import java.util.Objects;

/**
 * Data for emails asking the rider to return the vehicle (reminder before checkout or at checkout).
 */
public final class RiderCarReturnEmailPayload {

    private final Locale messageLocale;
    private final String recipientEmail;
    private final String riderFullName;
    private final String vehicleLabel;
    private final String ownerEmail;
    private final String checkoutFormatted;
    private final String returnLocationLine;
    private final String reservationDetailPath;

    public RiderCarReturnEmailPayload(
            final Locale messageLocale,
            final String recipientEmail,
            final String riderFullName,
            final String vehicleLabel,
            final String ownerEmail,
            final String checkoutFormatted,
            final String returnLocationLine,
            final String reservationDetailPath) {
        this.messageLocale = Objects.requireNonNull(messageLocale, "messageLocale");
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(riderFullName, "riderFullName");
        this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
        this.ownerEmail = ownerEmail == null ? "" : ownerEmail;
        this.checkoutFormatted = checkoutFormatted == null ? "" : checkoutFormatted;
        this.returnLocationLine = returnLocationLine == null ? "" : returnLocationLine;
        this.reservationDetailPath = Objects.requireNonNull(reservationDetailPath, "reservationDetailPath");
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

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getCheckoutFormatted() {
        return checkoutFormatted;
    }

    public String getReturnLocationLine() {
        return returnLocationLine;
    }

    /** Context-relative path, e.g. {@code /my-reservations/12}. */
    public String getReservationDetailPath() {
        return reservationDetailPath;
    }
}
