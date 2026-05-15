package ar.edu.itba.paw.services.policy;

import java.time.OffsetDateTime;

import ar.edu.itba.paw.models.domain.Reservation;

/** Chat availability window and history paging for reservation messages. */
public final class ReservationChatPolicy {

    private final int graceDaysAfterFinished;
    private final int historyPageSize;

    public static ReservationChatPolicy fromValidatedConfiguration(
            final int graceDaysAfterFinished, final int historyPageSize) {
        if (graceDaysAfterFinished < 0) {
            throw new IllegalArgumentException(
                    "app.reservation.chat.grace-days-after-finished must be >= 0, got " + graceDaysAfterFinished);
        }
        if (historyPageSize < 1) {
            throw new IllegalArgumentException(
                    "app.reservation.chat.history-page-size must be >= 1, got " + historyPageSize);
        }
        return new ReservationChatPolicy(graceDaysAfterFinished, historyPageSize);
    }

    private ReservationChatPolicy(final int graceDaysAfterFinished, final int historyPageSize) {
        this.graceDaysAfterFinished = graceDaysAfterFinished;
        this.historyPageSize = historyPageSize;
    }

    public int getGraceDaysAfterFinished() {
        return graceDaysAfterFinished;
    }

    public int getHistoryPageSize() {
        return historyPageSize;
    }

    public boolean isChatAvailable(final Reservation reservation, final OffsetDateTime now) {
        if (reservation == null || now == null) {
            return false;
        }
        if (!reservation.isPaymentApproved()) {
            return false;
        }
        final Reservation.Status status = reservation.getStatus();
        if (status != Reservation.Status.ACCEPTED
                && status != Reservation.Status.STARTED
                && status != Reservation.Status.FINISHED) {
            return false;
        }
        if (status == Reservation.Status.FINISHED) {
            final OffsetDateTime graceEnd = reservation.getEndDate().plusDays(graceDaysAfterFinished);
            return now.isBefore(graceEnd);
        }
        return true;
    }
}
