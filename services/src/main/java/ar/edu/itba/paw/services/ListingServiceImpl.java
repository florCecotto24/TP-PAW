package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ListingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;
    private final ListingAvailabilityDao listingAvailabilityDao;
    private final CarDao carDao;

    @Autowired
    public ListingServiceImpl(
            final ListingDao listingDao,
            final ListingAvailabilityDao listingAvailabilityDao,
            final CarDao carDao) {
        this.listingDao = listingDao;
        this.listingAvailabilityDao = listingAvailabilityDao;
        this.carDao = carDao;
    }

    @Override
    @Transactional
    public Listing createListing(
            final long carId,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description,
            final List<AvailabilityPeriod> availabilityPeriods) {
        if (availabilityPeriods == null || availabilityPeriods.isEmpty()) {
            throw new IllegalArgumentException("At least one availability period is required.");
        }
        for (final AvailabilityPeriod period : availabilityPeriods) {
            if (!period.isValidOrder()) {
                throw new IllegalArgumentException("Final date must be after or equal to the start date.");
            }
        }
        final Car car = carDao.getCarById(carId).orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        final String title = car.getBrand() + " " + car.getModel();

        final Listing listing = listingDao.createListing(carId, title, status, dayPrice, startPoint, description);
        for (final AvailabilityPeriod period : availabilityPeriods) {
            listingAvailabilityDao.create(
                    listing.getId(),
                    period.startInstantUtc(),
                    period.endExclusiveInstantUtc());
        }
        return listing;
    }

    @Override
    public Optional<Listing> getListingById(final long id) {
        return listingDao.getListingById(id);
    }

    @Override
    public List<ListingAvailability> findAvailabilityByListingId(final long listingId) {
        return listingAvailabilityDao.findByListingId(listingId);
    }

    @Override
    public List<Listing> getAllListings() {
        return listingDao.getAllListings();
    }

    @Override
    public List<Listing> searchListings(final ListingSearchCriteria criteria) {
        return listingDao.searchListings(criteria);
    }

    @Override
    public List<Listing> getCheapestListings(int limit) {
        return listingDao.getCheapestListings(limit);
    }

    @Override
    public List<Listing> getMostRecentListings(int limit) {
        return listingDao.getMostRecentListings(limit);
    }

    @Override
    public List<Listing> findSimilarListings(final long listingId, final int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero.");
        }

        final Listing listing = listingDao.getListingById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + listingId));
        final Car car = carDao.getCarById(listing.getCarId())
                .orElseThrow(() -> new IllegalStateException("Car not found for listing: " + listingId));

        return listingDao.findSimilarListings(
                listingId,
                car.getType(),
                car.getPowertrain(),
                car.getTransmission(),
                limit);
    }
}
