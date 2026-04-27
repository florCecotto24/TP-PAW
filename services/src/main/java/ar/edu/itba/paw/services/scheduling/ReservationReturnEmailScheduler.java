package ar.edu.itba.paw.services.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationService;

/**
 * Sends return reminders, checkout return nudges, and post-rental review invites to riders.
 * Cron expressions and zone are configured under {@code app.scheduler.return-emails.*}.
 */
@Component
public class ReservationReturnEmailScheduler {

    private final ReservationService reservationService;

    @Autowired
    public ReservationReturnEmailScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.reminder-cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:America/Argentina/Buenos_Aires}")
    public void sendReturnReminders() {
        reservationService.dispatchReturnReminderEmails();
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.checkout-cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:America/Argentina/Buenos_Aires}")
    public void sendReturnCheckoutEmails() {
        reservationService.dispatchReturnCheckoutEmails();
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.review-invite-cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:America/Argentina/Buenos_Aires}")
    public void sendRiderReviewInvites() {
        reservationService.dispatchRiderReviewInviteEmails();
    }
}
