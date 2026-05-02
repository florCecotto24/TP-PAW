package ar.edu.itba.paw.services.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationService;

/**
 * Sends payment proof due reminder emails when the deadline is within
 * {@code app.reservation.payment-proof-reminder-lead-hours} and payment is not yet approved.
 * Cron and zone: {@code app.scheduler.payment-proof-reminder.*}.
 */
@Component
public final class DuePaymentProofReminderScheduler {

    private final ReservationService reservationService;

    @Autowired
    public DuePaymentProofReminderScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "${app.scheduler.payment-proof-reminder.cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.payment-proof-reminder.zone:America/Argentina/Buenos_Aires}")
    public void sendDuePaymentProofReminders() {
        reservationService.dispatchDuePaymentProofReminderEmails();
    }
}
