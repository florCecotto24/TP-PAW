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
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.WallDateTimeParsing;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.persistence.ReservationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;
    private final ListingAvailabilityDao listingAvailabilityDao;
    private final CarDao carDao;
    private final ReservationDao reservationDao;

    @Autowired
    public ListingServiceImpl(
            final ListingDao listingDao,
            final ListingAvailabilityDao listingAvailabilityDao,
            final CarDao carDao,
            final ReservationDao reservationDao) {
        this.listingDao = listingDao;
        this.listingAvailabilityDao = listingAvailabilityDao;
        this.carDao = carDao;
        this.reservationDao = reservationDao;
    }

    @Override
    @Transactional
    public Listing createListing(
            final long carId,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> availabilityPeriods) {
        if (availabilityPeriods == null || availabilityPeriods.isEmpty()) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_REQUIRED);
        }
        for (final AvailabilityPeriod period : availabilityPeriods) {
            if (!period.isValidOrder()) {
                throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INVALID_ORDER);
            }
        }
        validateAvailabilityIncludesNoDatesBeforeToday(availabilityPeriods);
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new ListingValidationException(MessageKeys.LISTING_CAR_NOT_FOUND, carId));
        final String title = car.getBrand() + " " + car.getModel();

        final Listing listing = listingDao.createListing(
                carId, title, status, dayPrice, startPoint, description, checkInTime, checkOutTime);
        for (final AvailabilityPeriod period : mergeAdjacentAvailabilityPeriods(availabilityPeriods)) {
            listingAvailabilityDao.create(
                    listing.getId(),
                    period.getStartInclusive(),
                    period.getEndInclusive());
        }
        return listing;
    }

    private static void validateAvailabilityIncludesNoDatesBeforeToday(final List<AvailabilityPeriod> periods) {
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        for (final AvailabilityPeriod period : periods) {
            if (period.getStartInclusive().isBefore(today)) {
                throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INCLUDES_PAST_DATES);
            }
        }
    }

    private static List<AvailabilityPeriod> mergeAdjacentAvailabilityPeriods(final List<AvailabilityPeriod> periods) {
        final List<AvailabilityPeriod> sorted = periods.stream()
                .sorted(Comparator.comparing(AvailabilityPeriod::getStartInclusive))
                .collect(Collectors.toList());
        final List<AvailabilityPeriod> out = new ArrayList<>();
        AvailabilityPeriod cur = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            final AvailabilityPeriod next = sorted.get(i);
            if (!next.getStartInclusive().isAfter(cur.getEndInclusive().plusDays(1))) {
                final LocalDate newEnd = cur.getEndInclusive().isBefore(next.getEndInclusive())
                        ? next.getEndInclusive()
                        : cur.getEndInclusive();
                cur = new AvailabilityPeriod(cur.getStartInclusive(), newEnd);
            } else {
                out.add(cur);
                cur = next;
            }
        }
        out.add(cur);
        return out;
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
    public List<AvailabilityPeriod> getBookableWallAvailabilityPeriods(final long listingId) {
        final TreeSet<LocalDate> days = new TreeSet<>();
        for (final ListingAvailability la : findAvailabilityByListingId(listingId)) {
            LocalDate d = la.getStartInclusive();
            while (!d.isAfter(la.getEndInclusive())) {
                days.add(d);
                d = d.plusDays(1);
            }
        }
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        for (final Reservation r : reservationDao.findBlockingByListingId(listingId)) {
            LocalDate d = r.getStartDate().toInstant().atZone(wall).toLocalDate();
            final LocalDate until = r.getEndDate().toInstant().atZone(wall).toLocalDate();
            while (!d.isAfter(until)) {
                days.remove(d);
                d = d.plusDays(1);
            }
        }
        return mergeAdjacentWallDaysToPeriods(days);
    }

    private static List<AvailabilityPeriod> mergeAdjacentWallDaysToPeriods(final SortedSet<LocalDate> days) {
        if (days.isEmpty()) {
            return List.of();
        }
        final List<AvailabilityPeriod> out = new ArrayList<>();
        LocalDate segStart = null;
        LocalDate prev = null;
        for (final LocalDate d : days) {
            if (segStart == null) {
                segStart = d;
                prev = d;
            } else if (d.equals(prev.plusDays(1))) {
                prev = d;
            } else {
                out.add(new AvailabilityPeriod(segStart, prev));
                segStart = d;
                prev = d;
            }
        }
        out.add(new AvailabilityPeriod(segStart, prev));
        return List.copyOf(out);
    }

    @Override
    public boolean reservationIntervalFitsListingAvailability(
            final long listingId,
            final Long availabilityId,
            final OffsetDateTime startDate,
            final OffsetDateTime endDate) {
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        final LocalDate pickupDay = startDate.toInstant().atZone(wall).toLocalDate();
        final LocalDate returnDay = endDate.toInstant().atZone(wall).toLocalDate();
        final List<AvailabilityPeriod> bookable = getBookableWallAvailabilityPeriods(listingId);
        return everyWallDayCoveredByAvailabilityPeriods(pickupDay, returnDay, bookable);
    }

    private static boolean everyWallDayCoveredByAvailabilityPeriods(
            final LocalDate pickupDay,
            final LocalDate returnDay,
            final List<AvailabilityPeriod> periods) {
        if (periods.isEmpty()) {
            return false;
        }
        LocalDate d = pickupDay;
        while (!d.isAfter(returnDay)) {
            final LocalDate day = d;
            final boolean covered = periods.stream().anyMatch(
                    p -> !day.isBefore(p.getStartInclusive()) && !day.isAfter(p.getEndInclusive()));
            if (!covered) {
                return false;
            }
            d = d.plusDays(1);
        }
        return true;
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
                if ("UNDER_5000".equals(u) || "5000_TO_15000".equals(u)
                        || "15000_TO_30000".equals(u) || "OVER_30000".equals(u)) {
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
