package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Listing;

import java.math.BigDecimal;
import java.util.Optional;

public interface ListingService {

    Listing createListing(
            long carId,
            String title,
            Listing.Status status,
            BigDecimal dayPrice,
            String startPoint,
            String description);

    Optional<Listing> getListingById(long id);
}
