package ar.edu.itba.paw.scheduling;

import ar.edu.itba.paw.services.reservation.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks confirmed ({@code accepted}) reservations as {@code started} once their pickup
 * {@code start_date} is reached (cron from {@code app.scheduler.reservation-start.cron}).
 */
@Component
public final class ReservationStartTransitionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationStartTransitionScheduler.class);

    private final ReservationService reservationService;

    @Autowired
    public ReservationStartTransitionScheduler(final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Scheduled(cron = "${app.scheduler.reservation-start.cron:0 0/10 * * * ?}")
    public void transitionDueAcceptedReservationsToStarted() {
        try {
            reservationService.transitionAcceptedReservationsToStarted();
        } catch (final RuntimeException e) {
            LOGGER.atError().setCause(e).log("Reservation start transition failed");
        }
    }
}
