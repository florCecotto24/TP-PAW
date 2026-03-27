package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.persistence.ListingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;

    @Autowired
    public ListingServiceImpl(final ListingDao listingDao) {
        this.listingDao = listingDao;
    }

    @Override
    public Listing createListing(
            final long carId,
            final String title,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description) {
        return listingDao.createListing(carId, title, status, dayPrice, startPoint, description);
    }

    @Override
    public Optional<Listing> getListingById(final long id) {
        return listingDao.getListingById(id);
    }
}
