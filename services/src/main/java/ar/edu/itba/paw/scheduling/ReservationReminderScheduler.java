package ar.edu.itba.paw.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.reservation.ReservationService;

/**
 * Daily job: emails riders a reminder the day before pickup for reservations starting tomorrow.
 * Cron and zone: {@code app.scheduler.reservation-reminder.*}. The use case (candidate window,
 * batch pickup snapshots, claim-then-send per row) lives in the reservation service tier.
 */
@Component
public final class ReservationReminderScheduler {

    private final ReservationService reservationService;

    @Autowired
    public ReservationReminderScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "${app.scheduler.reservation-reminder.cron:0 0 9 * * ?}",
            zone = "${app.scheduler.reservation-reminder.zone:${app.scheduler.default-zone}}")
    public void sendReservationReminders() {
        reservationService.dispatchReservationReminderEmails();
    }
}
