package ar.edu.itba.paw.policy;

/** Limits for reservation chat message bodies. Config: {@code app.validation.reservation-message-max-length}. */
public final class ReservationMessageValidationPolicy {

    private final int bodyMaxLength;

    public static ReservationMessageValidationPolicy fromValidatedBodyMaxLength(final int bodyMaxLength) {
        if (bodyMaxLength < 1) {
            throw new IllegalArgumentException(
                    "app.validation.reservation-message-max-length must be >= 1, got " + bodyMaxLength);
        }
        return new ReservationMessageValidationPolicy(bodyMaxLength);
    }

    private ReservationMessageValidationPolicy(final int bodyMaxLength) {
        this.bodyMaxLength = bodyMaxLength;
    }

    public int getBodyMaxLength() {
        return bodyMaxLength;
    }
}
