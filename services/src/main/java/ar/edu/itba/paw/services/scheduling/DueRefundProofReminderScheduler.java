package ar.edu.itba.paw.services.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationService;

/**
 * Reminds the listing owner to upload a refund transfer proof before the deadline for cancelled confirmed reservations.
 * Uses the same lead window as payment-proof reminders ({@code app.reservation.payment-proof-reminder-lead-hours}).
 */
@Component
public final class DueRefundProofReminderScheduler {

    private final ReservationService reservationService;

    @Autowired
    public DueRefundProofReminderScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "${app.scheduler.refund-proof-reminder.cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.refund-proof-reminder.zone:America/Argentina/Buenos_Aires}")
    public void sendDueRefundProofReminders() {
        reservationService.dispatchDueRefundProofReminderEmails();
    }
}
