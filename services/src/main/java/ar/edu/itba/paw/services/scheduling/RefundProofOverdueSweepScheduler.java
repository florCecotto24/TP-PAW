package ar.edu.itba.paw.services.scheduling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.services.ReservationService;

/**
 * Blocks owners whose refund-proof deadlines have lapsed and dispatches the notification email.
 * Runs slightly less often than the reminder so transient race windows are avoided.
 */
@Component
public final class RefundProofOverdueSweepScheduler {

    private final ReservationService reservationService;

    @Autowired
    public RefundProofOverdueSweepScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(
            cron = "${app.scheduler.refund-proof-overdue-sweep.cron:0 5/15 * * * ?}",
            zone = "${app.scheduler.refund-proof-overdue-sweep.zone:${app.scheduler.default-zone}}")
    public void sweepRefundOverdueAndBlockOwners() {
        reservationService.sweepRefundOverdueAndBlockOwners();
    }
}
