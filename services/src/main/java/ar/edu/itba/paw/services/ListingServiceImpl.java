package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;
    private final CarDao carDao;

    @Autowired
    public ListingServiceImpl(final ListingDao listingDao, final CarDao carDao) {
        this.listingDao = listingDao;
        this.carDao = carDao;
    }

    @Override
    public Listing createListing(
            final long carId,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description) {
        Car car = carDao.getCarById(carId).orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        String title = car.getBrand() + " " + car.getModel();

        return listingDao.createListing(carId, title, status, dayPrice, startPoint, description);
    }

    @Override
    public Optional<Listing> getListingById(final long id) {
        return listingDao.getListingById(id);
    }

    @Override
    public List<Listing> getAllListings() {
        return listingDao.getAllListings();
    }
}
