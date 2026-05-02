package ar.edu.itba.paw.models.email;

import java.util.Objects;

import ar.edu.itba.paw.models.domain.Reservation;

/**
 * Mail data for reservation cancellation (rider + owner). Wraps shared {@link ReservationMailPayload} and requires the
 * reservation status after cancellation for template intro copy.
 */
public final class ReservationCancellationEmailPayload {

    private final ReservationMailPayload mail;
    private final Reservation.Status cancellationStatus;

    private ReservationCancellationEmailPayload(final Builder builder) {
        this.mail = Objects.requireNonNull(builder.mail, "mail");
        this.cancellationStatus = Objects.requireNonNull(builder.cancellationStatus, "cancellationStatus");
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

    public static final class Builder {
        private ReservationMailPayload mail;
        private Reservation.Status cancellationStatus;

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

        public ReservationCancellationEmailPayload build() {
            return new ReservationCancellationEmailPayload(this);
        }
    }
}
