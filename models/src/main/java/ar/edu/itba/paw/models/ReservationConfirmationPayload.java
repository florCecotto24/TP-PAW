package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

public final class ReservationConfirmationPayload {

    private final String recipientEmail;
    private final String riderFullName;
    private final long reservationId;
    private final long listingId;
    private final String vehicleLabel;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    /** Delivery + pickup for the email to the rider (without door number until payment proof). */
    private final String riderHandoverLocation;
    /** Full data for the email to the owner. */
    private final String ownerHandoverLocation;
    private final String ownerFullName;
    private final String ownerEmail;
    private final String reservationTotal;
    private final Locale riderMailLocale;
    private final Locale ownerMailLocale;

    public ReservationConfirmationPayload(
            final String recipientEmail,
            final String riderFullName,
            final long reservationId,
            final long listingId,
            final String vehicleLabel,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final String riderHandoverLocation,
            final String ownerHandoverLocation,
            final String ownerFullName,
            final String ownerEmail,
            final String reservationTotal,
            final Locale riderMailLocale,
            final Locale ownerMailLocale) {
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(riderFullName, "riderFullName");
        this.reservationId = reservationId;
        this.listingId = listingId;
        this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
        this.riderHandoverLocation = riderHandoverLocation;
        this.ownerHandoverLocation = ownerHandoverLocation;
        this.ownerFullName = ownerFullName;
        this.ownerEmail = ownerEmail;
        this.reservationTotal = reservationTotal;
        this.riderMailLocale = Objects.requireNonNull(riderMailLocale, "riderMailLocale");
        this.ownerMailLocale = Objects.requireNonNull(ownerMailLocale, "ownerMailLocale");
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRiderFullName() {
        return riderFullName;
    }

    public long getReservationId() {
        return reservationId;
    }

    public long getListingId() {
        return listingId;
    }

    public String getVehicleLabel() {
        return vehicleLabel;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public String getRiderHandoverLocation() {
        return riderHandoverLocation;
    }

    public String getOwnerHandoverLocation() {
        return ownerHandoverLocation;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public Locale getRiderMailLocale() {
        return riderMailLocale;
    }

    public Locale getOwnerMailLocale() {
        return ownerMailLocale;
    }

    public String getReservationTotal() {
        return reservationTotal;
    }

    public Locale getMessageLocale() {
        return riderMailLocale;
    }

    public String getDeliveryLocation() {
        return riderHandoverLocation;
    }
}
