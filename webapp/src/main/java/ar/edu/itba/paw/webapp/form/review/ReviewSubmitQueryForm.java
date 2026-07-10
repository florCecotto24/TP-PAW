package ar.edu.itba.paw.webapp.form.review;

import javax.validation.constraints.NotNull;

/**
 * Required query param for {@code POST /reviews?reservationId=…}.
 */
public final class ReviewSubmitQueryForm {

    @NotNull(message = "{validation.review.reservationId.required}")
    private Long reservationId;

    public static ReviewSubmitQueryForm of(final Long reservationId) {
        final ReviewSubmitQueryForm form = new ReviewSubmitQueryForm();
        form.reservationId = reservationId;
        return form;
    }

    public Long getReservationId() {
        return reservationId;
    }
}
