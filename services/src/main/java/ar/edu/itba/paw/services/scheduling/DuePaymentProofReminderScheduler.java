package ar.edu.itba.paw.services.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationService;

/**
 * Sends payment proof due reminder emails to riders when their payment proof deadline
 * is within 2 hours and payment has not yet been approved.
 */
@Component
public final class DuePaymentProofReminderScheduler {

    private final ReservationService reservationService;

    @Autowired
    public DuePaymentProofReminderScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "0 0/10 * * * ?",
            zone = "America/Argentina/Buenos_Aires")
    public void sendDuePaymentProofReminders() {
        reservationService.dispatchDuePaymentProofReminderEmails();
    }
}
