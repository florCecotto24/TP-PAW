package ar.edu.itba.paw.scheduling;

import ar.edu.itba.paw.services.reservation.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cancels reservations stuck in pending payment proof after the configured deadline (cron from
 * {@code app.scheduler.payment-proof.cron}).
 */
@Component
public final class PaymentProofSweepScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentProofSweepScheduler.class);

    private final ReservationService reservationService;

    @Autowired
    public PaymentProofSweepScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "${app.scheduler.payment-proof.cron:0 0/15 * * * ?}")
    public void cancelStalePendingPayment() {
        try {
            reservationService.cancelExpiredPendingPaymentReservations();
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).log("Payment proof sweep failed");
        }
    }
}
