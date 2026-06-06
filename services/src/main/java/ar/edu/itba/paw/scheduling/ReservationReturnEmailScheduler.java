package ar.edu.itba.paw.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.reservation.ReservationService;

/**
 * Sends return reminders, checkout return nudges, and post-rental review invites to riders.
 * Cron expressions and zone are configured under {@code app.scheduler.return-emails.*}.
 */
@Component
public final class ReservationReturnEmailScheduler {

    private final ReservationService reservationService;

    @Autowired
    public ReservationReturnEmailScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.reminder-cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:${app.scheduler.default-zone}}")
    public void sendReturnReminders() {
        reservationService.dispatchReturnReminderEmails();
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.checkout-cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:${app.scheduler.default-zone}}")
    public void sendReturnCheckoutEmails() {
        reservationService.dispatchReturnCheckoutEmails();
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.review-invite-cron:0 0/10 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:${app.scheduler.default-zone}}")
    public void sendRiderReviewInvites() {
        reservationService.dispatchRiderReviewInviteEmails();
    }

    @Scheduled(
            cron = "${app.scheduler.return-emails.review-auto-skip-cron:0 0/30 * * * ?}",
            zone = "${app.scheduler.return-emails.zone:${app.scheduler.default-zone}}")
    public void dispatchReviewAutoSkips() {
        reservationService.dispatchReviewAutoSkips();
    }
}
