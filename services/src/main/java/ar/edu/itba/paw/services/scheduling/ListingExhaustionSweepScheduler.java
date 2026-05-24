package ar.edu.itba.paw.services.scheduling;

import ar.edu.itba.paw.services.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * Pauses cars that are currently {@code ACTIVE} but have no bookable wall day from today onward,
 * so the public catalogue stops surfacing exhausted vehicles when time advances without owner action.
 * Cron and zone are read from configuration ({@code app.scheduler.listing-exhaustion.*}); the defaults coincide with
 * {@link AvailabilityPeriod#WALL_ZONE}.
 */
@Component
public final class ListingExhaustionSweepScheduler {

    private final CarService carService;

    @Autowired
    public ListingExhaustionSweepScheduler(final CarService carService) {
        this.carService = carService;
    }

    @Scheduled(
            cron = "${app.scheduler.listing-exhaustion.cron:0 0 4 * * ?}",
            zone = "${app.scheduler.listing-exhaustion.zone:America/Argentina/Buenos_Aires}")
    public void sweepExhaustedListings() {
        carService.refreshExhaustedCarsToPaused();
    }
}
