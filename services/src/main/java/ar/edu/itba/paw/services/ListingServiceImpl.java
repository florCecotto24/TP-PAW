package ar.edu.itba.paw.services;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.AvailabilityPeriod;
import ar.edu.itba.paw.models.Car;
import ar.edu.itba.paw.models.Image;
import ar.edu.itba.paw.models.HomeListingCards;
import ar.edu.itba.paw.models.Listing;
import ar.edu.itba.paw.models.ListingAvailability;
import ar.edu.itba.paw.models.ListingCard;
import ar.edu.itba.paw.models.ListingDetail;
import ar.edu.itba.paw.models.ListingSearchCriteria;
import ar.edu.itba.paw.models.Page;
import ar.edu.itba.paw.models.Reservation;
import ar.edu.itba.paw.models.User;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;


@Service
public class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;
    private final ListingAvailabilityDao listingAvailabilityDao;
    private final CarDao carDao;
    private final ReservationDao reservationDao;
    private final UserService userService;
    private final ImageService imageService;
    private final CarPictureService carPictureService;

    @Autowired
    public ListingServiceImpl(
            final ListingDao listingDao,
            final ListingAvailabilityDao listingAvailabilityDao,
            final CarDao carDao,
            final ReservationDao reservationDao,
            final UserService userService,
            final ImageService imageService,
            final CarPictureService carPictureService) {
        this.listingDao = listingDao;
        this.listingAvailabilityDao = listingAvailabilityDao;
        this.carDao = carDao;
        this.reservationDao = reservationDao;
        this.userService = userService;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
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
        validateListingCheckOutAfterCheckIn(checkInTime, checkOutTime);
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

    @Override
    @Transactional
    public CarPublicationResult publish(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission,
            final BigDecimal pricePerDay,
            final String startPoint,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> periods,
            final List<ImageUpload> images) {
        final User publisher = userService.getUserById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        final Car car = carDao.createCar(
                publisher.getId(),
                plate,
                brand,
                model,
                type,
                powertrain,
                transmission);
        final Listing listing = createListing(
                car.getId(),
                Listing.Status.ACTIVE,
                pricePerDay,
                startPoint,
                description,
                checkInTime,
                checkOutTime,
                periods);

        int displayOrder = 1;
        if (images != null) {
            for (final ImageUpload picture : images) {
                if (picture.getData() == null || picture.getData().length == 0) {
                    continue;
                }
                final Image image = imageService.createImage(
                        picture.getFilename(),
                        picture.getContentType(),
                        picture.getData());
                carPictureService.createCarPicture(car.getId(), image.getId(), displayOrder);
                displayOrder++;
            }
        }

        return new CarPublicationResult(publisher, car, listing);
    }

    private static void validateListingCheckOutAfterCheckIn(final LocalTime checkInTime, final LocalTime checkOutTime) {
        if (checkInTime == null || checkOutTime == null) {
            return;
        }
        if (!checkOutTime.isAfter(checkInTime)) {
            throw new ListingValidationException(MessageKeys.LISTING_CHECKOUT_NOT_AFTER_CHECKIN);
        }
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
    @Transactional(readOnly = true)
    public Optional<Listing> getListingById(final long id) {
        return listingDao.getListingById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListingDetail> getListingDetailById(final long id) {
        return listingDao.getListingDetailById(id);
    }

    @Override
    @Transactional
    public boolean updateOwnerListing(
            final long ownerId,
            final long listingId,
            final BigDecimal dayPrice,
            final String startPoint,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> availabilityPeriods) {
        validateListingCheckOutAfterCheckIn(checkInTime, checkOutTime);
        if (availabilityPeriods != null && !availabilityPeriods.isEmpty()) {
            for (final AvailabilityPeriod period : availabilityPeriods) {
                if (!period.isValidOrder()) {
                    throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INVALID_ORDER);
                }
            }
            validateAvailabilityIncludesNoDatesBeforeToday(availabilityPeriods);
        }
        final String safeStartPoint = startPoint == null ? "" : startPoint.trim();
        final String safeDescription = description == null ? "" : description.trim();
        boolean updated = listingDao.updateOwnerListing(
                ownerId,
                listingId,
                dayPrice,
                safeStartPoint,
                safeDescription,
                checkInTime,
                checkOutTime);
        if (updated && availabilityPeriods != null) {
            listingAvailabilityDao.deleteByListingId(listingId);
            for (final AvailabilityPeriod p : availabilityPeriods) {
                listingAvailabilityDao.create(listingId, p.getStartInclusive(), p.getEndInclusive());
            }
        }
        if (updated) {
            refreshListingFinishedIfExhausted(listingId);
        }
        return updated;
    }

    @Override
    @Transactional
    public void refreshListingFinishedIfExhausted(final long listingId) {
        final Optional<Listing> opt = listingDao.getListingById(listingId);
        if (opt.isEmpty()) {
            return;
        }
        final Listing.Status status = opt.get().getStatus();
        if (status != Listing.Status.ACTIVE && status != Listing.Status.PAUSED) {
            return;
        }
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final Set<Long> bookable = listingIdsWithAtLeastOneBookableWallDay(List.of(listingId), wall);
        if (!bookable.contains(listingId)) {
            listingDao.updateListingStatus(listingId, Listing.Status.FINISHED, Listing.Status.ACTIVE, Listing.Status.PAUSED);
        }
    }

    @Override
    @Transactional
    public void refreshExhaustedListingsToFinished() {
        final List<Long> ids = listingDao.findListingIdsWithStatuses(Listing.Status.ACTIVE, Listing.Status.PAUSED);
        if (ids.isEmpty()) {
            return;
        }
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final int batch = 200;
        for (int i = 0; i < ids.size(); i += batch) {
            final List<Long> slice = ids.subList(i, Math.min(i + batch, ids.size()));
            final Set<Long> bookable = listingIdsWithAtLeastOneBookableWallDay(slice, wall);
            for (final long id : slice) {
                if (!bookable.contains(id)) {
                    listingDao.updateListingStatus(id, Listing.Status.FINISHED, Listing.Status.ACTIVE, Listing.Status.PAUSED);
                }
            }
        }
    }

    @Override
    @Transactional
    public boolean toggleListingStatus(final long ownerId, final long listingId) {

        final Optional<Listing> listingOpt = listingDao.getListingById(listingId);
        final Optional<User> ownerOpt = userService.getUserById(ownerId);
        final boolean deleted = listingDao.toggleListingStatus(ownerId, listingId);
        if (!deleted || listingOpt.isEmpty() || ownerOpt.isEmpty()) {
            return deleted;
        }

        return true;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ListingAvailability> findAvailabilityByListingId(final long listingId) {
        return listingAvailabilityDao.findByListingId(listingId);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<Listing> getAllListings() {
        return listingDao.getAllListings();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> searchListings(final ListingSearchCriteria criteria) {
        final List<Listing> found = listingDao.searchListings(criteria);
        if (criteria.getBrowseWallDate() == null) {
            return found;
        }
        final Set<Long> bookableIds = listingIdsWithAtLeastOneBookableWallDay(
                found.stream().map(Listing::getId).collect(Collectors.toList()),
                criteria.getBrowseWallDate());
        return found.stream()
                .filter(l -> bookableIds.contains(l.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> getCheapestListings(int limit) {
        return listingDao.getCheapestListings(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Listing> getMostRecentListings(int limit) {
        return listingDao.getMostRecentListings(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> getCheapestListingCards(final int page, final int pageSize, final User viewer) {
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final Page<ListingCard> raw = listingDao.getCheapestListingCards(page, pageSize, wall, browseExcludeOwnerId(viewer));
        final List<ListingCard> filtered = retainBookableListingCards(raw.getContent(), wall);
        return new Page<>(filtered, raw.getCurrentPage(), raw.getPageSize(), raw.getTotalItems());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> getMostRecentListingCards(final int page, final int pageSize, final User viewer) {
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final Page<ListingCard> raw = listingDao.getMostRecentListingCards(page, pageSize, wall, browseExcludeOwnerId(viewer));
        final List<ListingCard> filtered = retainBookableListingCards(raw.getContent(), wall);
        return new Page<>(filtered, raw.getCurrentPage(), raw.getPageSize(), raw.getTotalItems());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> getOwnerListingCards(final long ownerId, final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = pageSize > 0 ? pageSize : PAGE_SIZE;
        return listingDao.getOwnerListingCards(ownerId, safePage, safePageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasListingsByOwner(final long ownerId) {
        return listingDao.hasListingsByOwner(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public HomeListingCards getHomeListingCards(final int limit, final User viewer) {
        if (limit <= 0) {
            throw new ListingValidationException(MessageKeys.LISTING_LIMIT_POSITIVE);
        }
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final HomeListingCards raw = listingDao.getHomeListingCards(limit, wall, browseExcludeOwnerId(viewer));
        return new HomeListingCards(
                retainBookableListingCards(raw.cheapest(), wall),
                retainBookableListingCards(raw.mostRecent(), wall));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        List<ListingCard> all = listingDao.searchListingCards(criteria);
        if (criteria.getBrowseWallDate() != null) {
            all = retainBookableListingCards(all, criteria.getBrowseWallDate());
        }
        final long total = all.size();
        final int offset = criteria.getPage() * criteria.getPageSize();
        final List<ListingCard> slice = all.subList(
                Math.min(offset, all.size()),
                Math.min(offset + criteria.getPageSize(), all.size()));
        return new Page<>(slice, criteria.getPage(), criteria.getPageSize(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingCard> findSimilarListingCards(final long listingId, final int limit, final User viewer) {
        if (limit <= 0) {
            throw new ListingValidationException(MessageKeys.LISTING_LIMIT_POSITIVE);
        }
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final int scan = Math.max(limit * 8, 32);
        final List<ListingCard> raw = listingDao.findSimilarListingCards(listingId, scan, wall, browseExcludeOwnerId(viewer));
        final List<ListingCard> filtered = retainBookableListingCards(raw, wall);
        return filtered.stream().limit(limit).collect(Collectors.toList());
    }

    private static final int PAGE_SIZE = 8;

    @Override
    public ListingSearchCriteria buildSearchCriteria(
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final List<String> price,
            final String from,
            final String until,
            final int page,
            final String sort,
            final User viewer) {
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
        final String[] sortParts = (sort != null && !sort.isBlank()) ? sort.split(",", 2) : new String[0];
        final String sortBy  = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        final LocalDate browseWallDate = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        return new ListingSearchCriteria(
                query, transmissions, powertrains, mergedCarTypes, bands,
                rangeStart, rangeEndExclusive,
                page, PAGE_SIZE, sortBy, sortDir,
                browseWallDate, browseExcludeOwnerId(viewer));
    }

    private static Long browseExcludeOwnerId(final User viewer) {
        return viewer != null ? viewer.getId() : null;
    }

    private Set<Long> listingIdsWithAtLeastOneBookableWallDay(final List<Long> listingIds, final LocalDate fromWall) {
        if (listingIds.isEmpty()) {
            return Set.of();
        }
        final List<ListingAvailability> overlapping =
                listingAvailabilityDao.findByListingIdsEndingOnOrAfter(listingIds, fromWall);
        final List<Reservation> blocking = reservationDao.findBlockingByListingIds(listingIds);
        final Map<Long, TreeSet<LocalDate>> daysByListing = new HashMap<>();
        for (final ListingAvailability la : overlapping) {
            final LocalDate start = la.getStartInclusive().isBefore(fromWall) ? fromWall : la.getStartInclusive();
            for (LocalDate d = start; !d.isAfter(la.getEndInclusive()); d = d.plusDays(1)) {
                daysByListing.computeIfAbsent(la.getListingId(), k -> new TreeSet<>()).add(d);
            }
        }
        final ZoneId wall = AvailabilityPeriod.WALL_ZONE;
        for (final Reservation r : blocking) {
            final TreeSet<LocalDate> days = daysByListing.get(r.getListingId());
            if (days == null || days.isEmpty()) {
                continue;
            }
            LocalDate d = r.getStartDate().toInstant().atZone(wall).toLocalDate();
            final LocalDate until = r.getEndDate().toInstant().atZone(wall).toLocalDate();
            while (!d.isAfter(until)) {
                days.remove(d);
                d = d.plusDays(1);
            }
        }
        final Set<Long> out = new HashSet<>();
        for (final Map.Entry<Long, TreeSet<LocalDate>> e : daysByListing.entrySet()) {
            if (!e.getValue().isEmpty()) {
                out.add(e.getKey());
            }
        }
        return out;
    }

    private List<ListingCard> retainBookableListingCards(final List<ListingCard> cards, final LocalDate fromWall) {
        if (cards.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = cards.stream().map(ListingCard::getListingId).distinct().collect(Collectors.toList());
        final Set<Long> ok = listingIdsWithAtLeastOneBookableWallDay(ids, fromWall);
        return cards.stream()
                .filter(c -> ok.contains(c.getListingId()))
                .collect(Collectors.toCollection(ArrayList::new));
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
