package ar.edu.itba.paw.models.email.reservation;

import java.util.Objects;

import ar.edu.itba.paw.models.domain.reservation.Reservation;

/**
 * Mail data for reservation cancellation (rider + optionally owner). Wraps shared {@link ReservationMailPayload} and
 * requires the reservation status after cancellation for template intro copy.
 * When the owner will receive a dedicated follow-up (e.g. refund proof obligation), set
 * {@link #isNotifyOwnerCancellation()} to {@code false} to avoid duplicate owner notifications.
 */
public final class ReservationCancellationEmailPayload {

    private final ReservationMailPayload mail;
    private final Reservation.Status cancellationStatus;
    private final boolean notifyOwnerCancellation;

    private ReservationCancellationEmailPayload(final Builder builder) {
        this.mail = Objects.requireNonNull(builder.mail, "mail");
        this.cancellationStatus = Objects.requireNonNull(builder.cancellationStatus, "cancellationStatus");
        this.notifyOwnerCancellation = builder.notifyOwnerCancellation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ReservationMailPayload getMail() {
        return mail;
    }

    public Reservation.Status getCancellationStatus() {
        return cancellationStatus;
    }

    public boolean isNotifyOwnerCancellation() {
        return notifyOwnerCancellation;
    }

    public static final class Builder {
        private ReservationMailPayload mail;
        private Reservation.Status cancellationStatus;
        private boolean notifyOwnerCancellation = true;

        private Builder() {
        }

        public Builder mail(final ReservationMailPayload value) {
            this.mail = value;
            return this;
        }

        public Builder cancellationStatus(final Reservation.Status value) {
            this.cancellationStatus = value;
            return this;
        }

        /**
         * When {@code false}, only the rider/client cancellation mail is sent; use when the owner gets another mail for
         * the same event (e.g. refund transfer proof).
         */
        public Builder notifyOwnerCancellation(final boolean value) {
            this.notifyOwnerCancellation = value;
            return this;
        }

        public ReservationCancellationEmailPayload build() {
            return new ReservationCancellationEmailPayload(this);
        }
    }
}
