package ar.edu.itba.paw.models.domain.reservation;

/**
 * The hat a signed-in user wears for a given reservation: the person who booked the car, or the
 * car's owner. Used to route role-scoped reservation operations (self-cancellation, reviews)
 * without resorting to {@code "rider"}/{@code "owner"} string literals at call sites.
 */
public enum ReservationParticipantRole {
    RIDER,
    OWNER
}
