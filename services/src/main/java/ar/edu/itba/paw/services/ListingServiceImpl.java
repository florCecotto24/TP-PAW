package ar.edu.itba.paw.services;

import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.HomeListingCards;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.models.WallDateTimeParsing;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ListingDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_REQUIRED);
        }
        for (final AvailabilityPeriod period : availabilityPeriods) {
            if (!period.isValidOrder()) {
                throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INVALID_ORDER);
            }
        }
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new ListingValidationException(MessageKeys.LISTING_CAR_NOT_FOUND, carId));
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
    public Optional<ListingDetail> getListingDetailById(final long id) {
        return listingDao.getListingDetailById(id);
    }

    @Override
    public List<ListingAvailability> findAvailabilityByListingId(final long listingId) {
        return listingAvailabilityDao.findByListingId(listingId);
    }

    @Override
    public boolean reservationIntervalFitsListingAvailability(
            final long listingId,
            final Long availabilityId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final List<ListingAvailability> availabilities = findAvailabilityByListingId(listingId);
        if (availabilities.isEmpty()) {
            return false;
        }
        if (availabilityId != null) {
            return availabilities.stream()
                    .filter(a -> a.getId() == availabilityId)
                    .findFirst()
                    .map(a -> isInsideAvailabilityWindow(a, startDate, endDate))
                    .orElse(false);
        }
        return availabilities.stream().anyMatch(a -> isInsideAvailabilityWindow(a, startDate, endDate));
    }

    private static boolean isInsideAvailabilityWindow(
            final ListingAvailability availability,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        return !startDate.isBefore(availability.getStartDate()) && !endDate.isAfter(availability.getEndDate());
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
    public List<ListingCard> getCheapestListingCards(final int limit) {
        return listingDao.getCheapestListingCards(limit);
    }

    @Override
    public List<ListingCard> getMostRecentListingCards(final int limit) {
        return listingDao.getMostRecentListingCards(limit);
    }

    @Override
    public HomeListingCards getHomeListingCards(final int limit) {
        if (limit <= 0) {
            throw new ListingValidationException(MessageKeys.LISTING_LIMIT_POSITIVE);
        }
        return listingDao.getHomeListingCards(limit);
    }

    @Override
    public List<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        return listingDao.searchListingCards(criteria);
    }

    @Override
    public List<ListingCard> findSimilarListingCards(final long listingId, final int limit) {
        if (limit <= 0) {
            throw new ListingValidationException(MessageKeys.LISTING_LIMIT_POSITIVE);
        }
        return listingDao.findSimilarListingCards(listingId, limit);
    }

    @Override
    public ListingSearchCriteria buildSearchCriteria(
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final List<String> price,
            final String from,
            final String until) {
        final List<String> transmissions = collectTransmissionParams(transmission);
        final List<String> powertrains = collectPowertrainParams(powertrain);
        final List<String> mergedCarTypes = collectCarTypeParams(category);
        final List<String> bands = new ArrayList<>();
        if (price != null) {
            for (final String p : price) {
                if (p == null || p.isBlank()) {
                    continue;
                }
                final String u = p.trim().toUpperCase();
                if ("FREE".equals(u) || "PAID".equals(u)) {
                    bands.add(u);
                }
            }
        }
        Instant rangeStart = WallDateTimeParsing.parseSearchFilterRangeStartInstant(from);
        Instant rangeEndExclusive = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(until);
        if (rangeStart != null && rangeEndExclusive != null && !rangeEndExclusive.isAfter(rangeStart)) {
            final Instant rs = WallDateTimeParsing.parseSearchFilterRangeStartInstant(until);
            final Instant re = WallDateTimeParsing.parseSearchFilterRangeEndExclusiveInstant(from);
            rangeStart = rs;
            rangeEndExclusive = re;
        }
        if (rangeStart == null || rangeEndExclusive == null || !rangeEndExclusive.isAfter(rangeStart)) {
            rangeStart = null;
            rangeEndExclusive = null;
        }
        return new ListingSearchCriteria(
                query, transmissions, powertrains, mergedCarTypes, bands, rangeStart, rangeEndExclusive);
    }

    private static List<String> collectCarTypeParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Type.valueOf(u);
                if (!out.contains(u)) {
                    out.add(u);
                }
            } catch (final IllegalArgumentException ignored) {
                // ignore invalid enum
            }
        }
        return out;
    }

    private static List<String> collectTransmissionParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Transmission.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ignored) {
                // ignore
            }
        }
        return out;
    }

    private static List<String> collectPowertrainParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Powertrain.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ignored) {
                // ignore
            }
        }
        return out;
    }
}
