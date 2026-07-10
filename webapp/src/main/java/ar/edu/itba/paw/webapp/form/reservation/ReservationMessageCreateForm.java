package ar.edu.itba.paw.webapp.form.reservation;

import ar.edu.itba.paw.webapp.validation.constraint.reservation.ReservationMessageHasContent;

/**
 * Multipart POST body for {@code /reservations/{id}/messages} before attachment bytes are read.
 */
@ReservationMessageHasContent
public final class ReservationMessageCreateForm {

    private String body;
    private boolean hasAttachment;

    public ReservationMessageCreateForm() {
    }

    public static ReservationMessageCreateForm of(final String body, final boolean hasAttachment) {
        final ReservationMessageCreateForm form = new ReservationMessageCreateForm();
        form.body = body;
        form.hasAttachment = hasAttachment;
        return form;
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(final boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }
}
