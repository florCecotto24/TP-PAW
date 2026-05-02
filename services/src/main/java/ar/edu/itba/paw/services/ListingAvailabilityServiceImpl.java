package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Pass-through to {@link ListingAvailabilityDao}; joins the caller’s transaction when one is active. */
@Service
public final class ListingAvailabilityServiceImpl implements ListingAvailabilityService {

    private final ListingAvailabilityDao listingAvailabilityDao;

    @Autowired
    public ListingAvailabilityServiceImpl(final ListingAvailabilityDao listingAvailabilityDao) {
        this.listingAvailabilityDao = listingAvailabilityDao;
    }

    @Override
    public ListingAvailability create(final long listingId, final LocalDate startInclusive, final LocalDate endInclusive) {
        return listingAvailabilityDao.create(listingId, startInclusive, endInclusive);
    }

    @Override
    public List<ListingAvailability> findByListingId(final long listingId) {
        return listingAvailabilityDao.findByListingId(listingId);
    }

    @Override
    public List<ListingAvailability> findByListingIdsEndingOnOrAfter(
            final Collection<Long> listingIds,
            final LocalDate minEndDate) {
        return listingAvailabilityDao.findByListingIdsEndingOnOrAfter(listingIds, minEndDate);
    }

    @Override
    public void deleteByListingId(final long listingId) {
        listingAvailabilityDao.deleteByListingId(listingId);
    }
}
