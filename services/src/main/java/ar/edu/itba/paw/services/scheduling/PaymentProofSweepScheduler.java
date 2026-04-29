package ar.edu.itba.paw.services.scheduling;

import ar.edu.itba.paw.services.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
        } catch (final Exception e) {
            LOGGER.atError().log("payment proof sweep failed", e);
        }
    }
}
