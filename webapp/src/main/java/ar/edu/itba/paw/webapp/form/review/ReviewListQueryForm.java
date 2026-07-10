package ar.edu.itba.paw.webapp.form.review;

import ar.edu.itba.paw.webapp.validation.constraint.review.ValidReviewListQuery;

/**
 * Query params for {@code GET /reviews} ({@code carId}, {@code recipientUserId}, or {@code reservationId}).
 */
@ValidReviewListQuery
public final class ReviewListQueryForm {

    public enum Filter {
        CAR,
        RECIPIENT_USER,
        RESERVATION
    }

    private Long carId;
    private Long recipientUserId;
    private Long reservationId;

    public static ReviewListQueryForm of(
            final Long carId, final Long recipientUserId, final Long reservationId) {
        final ReviewListQueryForm form = new ReviewListQueryForm();
        form.carId = carId;
        form.recipientUserId = recipientUserId;
        form.reservationId = reservationId;
        return form;
    }

    public Long getCarId() {
        return carId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Filter filter() {
        if (carId != null) {
            return Filter.CAR;
        }
        if (recipientUserId != null) {
            return Filter.RECIPIENT_USER;
        }
        return Filter.RESERVATION;
    }

    public long requireCarId() {
        return carId;
    }

    public long requireRecipientUserId() {
        return recipientUserId;
    }

    public long requireReservationId() {
        return reservationId;
    }
}
