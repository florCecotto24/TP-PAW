package ar.edu.itba.paw.services.scheduling;

import ar.edu.itba.paw.services.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.models.domain.AvailabilityPeriod;

/**
 * Turns into finished the listings that are active or paused and have no bookable wall day from today onward
 * (same rule as the public listing), if the time advances without editing or booking.
 * Cron and zone are read from configuration ({@code app.scheduler.listing-exhaustion.*}); the defaults coincide with
 * {@link AvailabilityPeriod#WALL_ZONE}.
 */
@Component
public class ListingExhaustionSweepScheduler {

    private final ListingService listingService;

    @Autowired
    public ListingExhaustionSweepScheduler(final ListingService listingService) {
        this.listingService = listingService;
    }

    @Scheduled(
            cron = "${app.scheduler.listing-exhaustion.cron:0 0 4 * * ?}",
            zone = "${app.scheduler.listing-exhaustion.zone:America/Argentina/Buenos_Aires}")
    public void sweepExhaustedListings() {
        listingService.refreshExhaustedListingsToFinished();
    }
}
