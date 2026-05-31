package ar.edu.itba.paw.services.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationMessageService;

/**
 * Sends hourly digest emails for reservation chat messages pending notification.
 * Cron expression and zone are configured under {@code app.scheduler.chat-digest.*}.
 */
@Component
public final class ReservationChatDigestScheduler {

    private final ReservationMessageService reservationMessageService;

    @Autowired
    public ReservationChatDigestScheduler(final ReservationMessageService reservationMessageService) {
        this.reservationMessageService = reservationMessageService;
    }

    @Scheduled(
            cron = "${app.scheduler.chat-digest.cron:0 0 * * * ?}",
            zone = "${app.scheduler.chat-digest.zone:${app.scheduler.default-zone}}")
    public void sendChatDigestEmails() {
        reservationMessageService.dispatchChatDigestEmails();
    }
}
