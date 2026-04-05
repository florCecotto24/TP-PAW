package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class ReservationConfirmationPayload {

    private final String recipientEmail;
    private final String riderFullName;
    private final long reservationId;
    private final long listingId;
    private final String vehicleLabel;
    private final OffsetDateTime startDate;
    private final OffsetDateTime endDate;
    private final String deliveryLocation;
    private final String ownerFullName;
    private final String ownerEmail;

    public ReservationConfirmationPayload(
            final String recipientEmail,
            final String riderFullName,
            final long reservationId,
            final long listingId,
            final String vehicleLabel,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate,
            final String deliveryLocation,
            final String ownerFullName,
            final String ownerEmail) {
        this.recipientEmail = Objects.requireNonNull(recipientEmail, "recipientEmail");
        this.riderFullName = Objects.requireNonNull(riderFullName, "riderFullName");
        this.reservationId = reservationId;
        this.listingId = listingId;
        this.vehicleLabel = Objects.requireNonNull(vehicleLabel, "vehicleLabel");
        this.startDate = Objects.requireNonNull(startDate, "startDate");
        this.endDate = Objects.requireNonNull(endDate, "endDate");
        this.deliveryLocation = deliveryLocation;
        this.ownerFullName = ownerFullName;
        this.ownerEmail = ownerEmail;
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

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }
}
