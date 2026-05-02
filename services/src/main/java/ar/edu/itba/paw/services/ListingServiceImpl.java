package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.CarPublicationResult;
import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.AvailabilityRiderLeadViolationException;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.dto.HomeListingCards;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.dto.ListingCard;
import ar.edu.itba.paw.models.dto.ListingDetail;
import ar.edu.itba.paw.models.pagination.DualLayerPageWindow;
import ar.edu.itba.paw.models.util.BookableWallAvailabilityCalendar;
import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.models.util.RiderPickupLeadTime;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.email.ListingPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.util.WallDateTimeParsing;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingAvailabilityDao;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.services.util.NeighborhoodNameMatcher;
import ar.edu.itba.paw.persistence.ReservationDao;
import ar.edu.itba.paw.services.pagination.ListingBrowsePagination;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.policy.ListingCheckInOutPolicy;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

@Service
public final class ListingServiceImpl implements ListingService {

    private final ListingDao listingDao;
    private final ListingAvailabilityDao listingAvailabilityDao;
    private final CarDao carDao;
    private final ReservationDao reservationDao;
    private final UserService userService;
    private final ImageService imageService;
    private final CarPictureService carPictureService;
    private final EmailService emailService;
    private final LocationService locationService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ListingCheckInOutPolicy listingCheckInOutPolicy;
    private final ListingAvailabilityPolicy listingAvailabilityPolicy;
    private final PaginationPolicy paginationPolicy;
    private final ListingBrowsePagination listingBrowsePagination;

    @Autowired
    public ListingServiceImpl(
            final ListingDao listingDao,
            final ListingAvailabilityDao listingAvailabilityDao,
            final CarDao carDao,
            final ReservationDao reservationDao,
            final UserService userService,
            final ImageService imageService,
            final CarPictureService carPictureService,
            final EmailService emailService,
            final LocationService locationService,
            final ReservationTimingPolicy reservationTimingPolicy,
            final ListingCheckInOutPolicy listingCheckInOutPolicy,
            final ListingAvailabilityPolicy listingAvailabilityPolicy,
            final PaginationPolicy paginationPolicy,
            final ListingBrowsePagination listingBrowsePagination) {
        this.listingDao = listingDao;
        this.listingAvailabilityDao = listingAvailabilityDao;
        this.carDao = carDao;
        this.reservationDao = reservationDao;
        this.userService = userService;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
        this.emailService = emailService;
        this.locationService = locationService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.listingCheckInOutPolicy = listingCheckInOutPolicy;
        this.listingAvailabilityPolicy = listingAvailabilityPolicy;
        this.paginationPolicy = paginationPolicy;
        this.listingBrowsePagination = listingBrowsePagination;
    }

    @Override
    @Transactional
    public Listing createListing(
            final long carId,
            final Listing.Status status,
            final BigDecimal dayPrice,
            final String startPointStreet,
            final String startPointNumber,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> availabilityPeriods,
            final Long neighborhoodId) {
        if (availabilityPeriods == null || availabilityPeriods.isEmpty()) {
            throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_REQUIRED);
        }
        validateListingCheckOutAfterCheckIn(checkInTime, checkOutTime);
        for (final AvailabilityPeriod period : availabilityPeriods) {
            if (!period.isValidOrder()) {
                throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INVALID_ORDER);
            }
        }
        listingAvailabilityPolicy.validateAvailabilityWithinPublishHorizon(
                LocalDate.now(AvailabilityPeriod.WALL_ZONE), availabilityPeriods);
        validateAvailabilityIncludesNoDatesBeforeToday(availabilityPeriods);
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new ListingValidationException(MessageKeys.LISTING_CAR_NOT_FOUND, carId));
        final String title = car.getBrand() + " " + car.getModel();

        final Listing listing = listingDao.createListing(
                carId, title, status, dayPrice, startPointStreet, startPointNumber, description, checkInTime, checkOutTime,
                neighborhoodId);
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
            final String startPointStreet,
            final String startPointNumber,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> periods,
            final List<ImageUpload> images,
            final Long neighborhoodId) {
        validatePickupAddress(neighborhoodId, startPointStreet, startPointNumber);
        final User publisher = userService.getUserById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!userService.hasValidCbu(publisher)) {
            throw new ListingValidationException(
                    MessageKeys.LISTING_PUBLISH_CBU_REQUIRED, CbuRules.REQUIRED_DIGIT_LENGTH);
        }
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
                startPointStreet,
                startPointNumber,
                description,
                checkInTime,
                checkOutTime,
                periods,
                neighborhoodId);

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

    private void validatePickupAddress(
            final Long neighborhoodId,
            final String pickupStreet,
            final String pickupStreetNumber) {
        if (neighborhoodId == null || pickupStreet == null || pickupStreet.isBlank()
                || pickupStreetNumber == null || pickupStreetNumber.isBlank()) {
            throw new ListingValidationException(MessageKeys.LISTING_PICKUP_LOCATION_REQUIRED);
        }
        final String trimmedNumber = pickupStreetNumber.trim();
        if (!trimmedNumber.matches("^[0-9]+$")) {
            throw new ListingValidationException(MessageKeys.LISTING_PICKUP_STREET_NUMBER_DIGITS_ONLY);
        }
        if (trimmedNumber.length() > 10) {
            throw new ListingValidationException(MessageKeys.LISTING_PICKUP_STREET_NUMBER_MAX_DIGITS);
        }
        locationService.findNeighborhoodById(neighborhoodId)
                .orElseThrow(() -> new ListingValidationException(MessageKeys.LISTING_PICKUP_LOCATION_REQUIRED));
    }

    private void validateListingCheckOutAfterCheckIn(final LocalTime checkInTime, final LocalTime checkOutTime) {
        if (checkInTime == null || checkOutTime == null) {
            return;
        }
        if (!checkOutTime.isAfter(checkInTime)) {
            throw new ListingValidationException(MessageKeys.LISTING_CHECKOUT_NOT_AFTER_CHECKIN);
        }
        if (!listingCheckInOutPolicy.hasMinimumGap(checkInTime, checkOutTime)) {
            throw new ListingValidationException(
                    MessageKeys.LISTING_CHECKINOUT_MIN_GAP,
                    listingCheckInOutPolicy.getMinHoursBetweenCheckInAndCheckOut());
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

    private static void validateUpdateAvailabilityNoFullyPastPeriods(final List<AvailabilityPeriod> periods) {
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        for (final AvailabilityPeriod period : periods) {
            if (period.getEndInclusive().isBefore(today)) {
                throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INCLUDES_PAST_DATES);
            }
        }
    }

    private static boolean availabilityMatchesExisting(
            final List<AvailabilityPeriod> submitted,
            final List<ListingAvailability> existing) {
        final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final List<ListingAvailability> nonPastExisting = existing.stream()
                .filter(la -> !la.getEndInclusive().isBefore(today))
                .sorted(Comparator.comparing(ListingAvailability::getStartInclusive))
                .collect(Collectors.toList());
        if (submitted.size() != nonPastExisting.size()) {
            return false;
        }
        final List<AvailabilityPeriod> sortedSubmitted = submitted.stream()
                .sorted(Comparator.comparing(AvailabilityPeriod::getStartInclusive))
                .collect(Collectors.toList());
        for (int i = 0; i < sortedSubmitted.size(); i++) {
            final AvailabilityPeriod s = sortedSubmitted.get(i);
            final ListingAvailability e = nonPastExisting.get(i);
            if (!s.getStartInclusive().equals(e.getStartInclusive())
                    || !s.getEndInclusive().equals(e.getEndInclusive())) {
                return false;
            }
        }
        return true;
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
            final String startPointStreet,
            final String startPointNumber,
            final String description,
            final LocalTime checkInTime,
            final LocalTime checkOutTime,
            final List<AvailabilityPeriod> availabilityPeriods,
            final Long neighborhoodId) {
        validateListingCheckOutAfterCheckIn(checkInTime, checkOutTime);
        if (availabilityPeriods != null && !availabilityPeriods.isEmpty()) {
            for (final AvailabilityPeriod period : availabilityPeriods) {
                if (!period.isValidOrder()) {
                    throw new ListingValidationException(MessageKeys.LISTING_AVAILABILITY_INVALID_ORDER);
                }
            }
            listingAvailabilityPolicy.validateAvailabilityWithinPublishHorizon(
                    LocalDate.now(AvailabilityPeriod.WALL_ZONE), availabilityPeriods);
            validateUpdateAvailabilityNoFullyPastPeriods(availabilityPeriods);
        }
        final String safeStartStreet = startPointStreet == null ? "" : startPointStreet.trim();
        final String safeDescription = description == null ? "" : description.trim();
        final Optional<Listing> existingOpt = listingDao.getListingById(listingId);
        final Long effNeighborhoodId = neighborhoodId != null ? neighborhoodId : existingOpt.flatMap(Listing::getNeighborhoodId).orElse(null);
        final String pickupStreet = !safeStartStreet.isBlank()
                ? safeStartStreet
                : existingOpt.map(Listing::getStartPointStreet).map(String::trim).orElse("");
        final String pickupNum = (startPointNumber != null && !startPointNumber.isBlank())
                ? startPointNumber.trim()
                : existingOpt.flatMap(Listing::getStartPointNumber).map(String::trim).orElse("");
        final boolean anyAddress = effNeighborhoodId != null || !pickupStreet.isBlank() || !pickupNum.isBlank();
        if (anyAddress) {
            if (effNeighborhoodId == null || pickupStreet.isBlank() || pickupNum.isBlank()) {
                throw new ListingValidationException(MessageKeys.LISTING_PICKUP_LOCATION_REQUIRED);
            }
            validatePickupAddress(effNeighborhoodId, pickupStreet, pickupNum);
        }
        boolean updated = listingDao.updateOwnerListing(
                ownerId,
                listingId,
                dayPrice,
                pickupStreet,
                pickupNum,
                safeDescription,
                checkInTime,
                checkOutTime,
                effNeighborhoodId);
        if (updated && availabilityPeriods != null) {
            final List<ListingAvailability> existing = listingAvailabilityDao.findByListingId(listingId);
            if (!availabilityMatchesExisting(availabilityPeriods, existing)) {
                listingAvailabilityDao.deleteByListingId(listingId);
                final LocalDate today = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
                for (final ListingAvailability la : existing) {
                    if (la.getEndInclusive().isBefore(today)) {
                        listingAvailabilityDao.create(listingId, la.getStartInclusive(), la.getEndInclusive());
                    }
                }
                for (final AvailabilityPeriod p : availabilityPeriods) {
                    listingAvailabilityDao.create(listingId, p.getStartInclusive(), p.getEndInclusive());
                }
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
        if (status != Listing.Status.ACTIVE) {
            return;
        }
        final LocalDate wall = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
        final Set<Long> bookable = listingIdsWithAtLeastOneBookableWallDay(List.of(listingId), wall);
        if (!bookable.contains(listingId)) {
            listingDao.updateListingStatus(
                    listingId,
                    Listing.Status.PAUSED,
                    Listing.Status.ACTIVE);
        }
    }

    @Override
    @Transactional
    public void refreshExhaustedListingsToFinished() {
        final List<Long> ids = listingDao.findListingIdsWithStatuses(Listing.Status.ACTIVE);
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
                    listingDao.updateListingStatus(
                            id,
                            Listing.Status.PAUSED,
                            Listing.Status.ACTIVE);
                }
            }
        }
    }

    @Override
    @Transactional
    public boolean toggleListingStatus(final long ownerId, final long listingId) {
        final Optional<Listing> listingOpt = listingDao.getListingById(listingId);
        if (listingOpt.isEmpty()) {
            return false;
        }
        final Listing listing = listingOpt.get();
        final Optional<Car> carOpt = carDao.getCarById(listing.getCarId());
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            return false;
        }
        if (listing.getStatus() == Listing.Status.PAUSED) {
            final Optional<User> ownerRow = userService.getUserById(ownerId);
            if (ownerRow.isEmpty() || !userService.hasValidCbu(ownerRow.get())) {
                throw new ListingValidationException(
                        MessageKeys.LISTING_ACTIVATE_CBU_REQUIRED, CbuRules.REQUIRED_DIGIT_LENGTH);
            }
        }
        return listingDao.toggleListingStatus(ownerId, listingId);
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

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityPeriod> getBookableWallAvailabilityPeriodsForRiderDatePicker(
            final long listingId, final LocalTime listingPickupWall, final Instant now) {
        final List<AvailabilityPeriod> bookable = getBookableWallAvailabilityPeriods(listingId);
        final List<AvailabilityPeriod> merged = BookableWallAvailabilityCalendar.mergeAdjacentPeriods(bookable);
        final int leadHours = reservationTimingPolicy.getPickupLeadHours();
        final Instant minPickupExclusive = now.plus(leadHours, ChronoUnit.HOURS);
        return BookableWallAvailabilityCalendar.clipPeriodsToMinPickupInstant(
                merged, listingPickupWall, AvailabilityPeriod.WALL_ZONE, minPickupExclusive);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate getPublicationMinAvailabilityFirstWallDay(
            final LocalTime listingPickupWall, final Instant now) {
        final LocalTime pickup = listingPickupWall != null ? listingPickupWall : Listing.DEFAULT_CHECK_IN_TIME;
        return RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup,
                AvailabilityPeriod.WALL_ZONE,
                now,
                reservationTimingPolicy.getPickupLeadHours());
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePublicationAvailabilityRiderLead(
            final List<AvailabilityPeriod> periods, final LocalTime checkInTime, final Instant now) {
        if (periods == null || periods.isEmpty()) {
            return;
        }
        final int pickupLeadHours = reservationTimingPolicy.getPickupLeadHours();
        final LocalTime pickup = checkInTime != null ? checkInTime : Listing.DEFAULT_CHECK_IN_TIME;
        final LocalDate minStart = RiderPickupLeadTime.minListingAvailabilityFirstDayInclusive(
                pickup, AvailabilityPeriod.WALL_ZONE, now, pickupLeadHours);
        for (int i = 0; i < periods.size(); i++) {
            final LocalDate from = periods.get(i).getStartInclusive();
            if (from.isBefore(minStart)) {
                throw new AvailabilityRiderLeadViolationException(
                        i, "validation.availabilityRow.from.riderLeadTime", minStart, pickupLeadHours);
            }
        }
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
    public Page<ListingCard> getCheapestListingCards(final int uiPage, final int pageSize, final User viewer) {
        return pagedBrowseListingCards(
                uiPage,
                pageSize,
                browseExcludeOwnerId(viewer),
                (offset, limit, wall, ex) -> listingDao.getCheapestListingCardsWindow(offset, limit, wall, ex));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> getMostRecentListingCards(final int uiPage, final int pageSize, final User viewer) {
        return pagedBrowseListingCards(
                uiPage,
                pageSize,
                browseExcludeOwnerId(viewer),
                (offset, limit, wall, ex) -> listingDao.getMostRecentListingCardsWindow(offset, limit, wall, ex));
    }

    @FunctionalInterface
    private interface ListingCardWindowQuery {
        List<ListingCard> load(int offset, int limit, LocalDate wall, Long excludeOwnerUserId);
    }

    private Page<ListingCard> pagedBrowseListingCards(
            final int uiPage,
            final int pageSize,
            final Long excludeOwnerUserId,
            final ListingCardWindowQuery windowQuery) {
        final DualLayerPageWindow w = listingBrowsePagination.window(uiPage, pageSize);
        final LocalDate wall = publicBrowseMinBookableWallDate();
        final long total = listingDao.countBrowseEligibleActiveListings(wall, excludeOwnerUserId);
        final List<ListingCard> batch = windowQuery.load(w.sqlOffset(), w.sqlLimit(), wall, excludeOwnerUserId);
        final List<ListingCard> slice = DualLayerPageWindow.sliceBatch(batch, w);
        final List<ListingCard> filtered = retainBookableListingCards(slice, wall);
        return new Page<>(filtered, w.uiPage(), w.uiPageSize(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> getOwnerListingCards(final OwnerListingSearchCriteria criteria) {
        return listingDao.getOwnerListingCards(criteria);
    }

    @Override
    public OwnerListingSearchCriteria buildOwnerListingSearchCriteria(
            final long ownerId,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final List<String> price,
            final List<String> listingStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final String sort) {
        final List<String> carTypes = collectCarTypeParams(category);
        final List<String> transmissions = collectTransmissionParams(transmission);
        final List<String> powertrains = collectPowertrainParams(powertrain);
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
        final List<String> statuses = new ArrayList<>();
        if (listingStatus != null) {
            for (final String s : listingStatus) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                final String low = s.trim().toLowerCase();
                if (LISTING_STATUSES.contains(low)) {
                    statuses.add(low);
                }
            }
        }
        final List<String> ratingBands = collectRatingBandParams(rating);
        final String[] sortParts = (sort != null && !sort.isBlank()) ? sort.split(",", 2) : new String[0];
        final String sortBy = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        return new OwnerListingSearchCriteria(
                ownerId, page, paginationPolicy.getDefaultPageSize(), statuses, textQuery,
                carTypes, transmissions, powertrains, bands, ratingBands, sortBy, sortDir);
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
        final LocalDate wall = publicBrowseMinBookableWallDate();
        final HomeListingCards raw = listingDao.getHomeListingCards(limit, wall, browseExcludeOwnerId(viewer));
        return new HomeListingCards(
                retainBookableListingCards(raw.cheapest(), wall),
                retainBookableListingCards(raw.mostRecent(), wall));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingCard> searchListingCards(final ListingSearchCriteria criteria) {
        final Page<ListingCard> raw = listingDao.searchListingCards(criteria);
        if (criteria.getBrowseWallDate() == null) {
            return raw;
        }
        final List<ListingCard> filtered = retainBookableListingCards(raw.getContent(), criteria.getBrowseWallDate());
        return new Page<>(filtered, raw.getCurrentPage(), raw.getPageSize(), raw.getTotalItems());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingCard> findSimilarListingCards(final long listingId, final int limit, final User viewer) {
        if (limit <= 0) {
            throw new ListingValidationException(MessageKeys.LISTING_LIMIT_POSITIVE);
        }
        final LocalDate wall = publicBrowseMinBookableWallDate();
        final int scan = Math.max(limit * 8, 32);
        final List<ListingCard> raw = listingDao.findSimilarListingCards(listingId, scan, wall, browseExcludeOwnerId(viewer));
        final List<ListingCard> filtered = retainBookableListingCards(raw, wall);
        return filtered.stream().limit(limit).collect(Collectors.toList());
    }

    private static final Set<String> LISTING_STATUSES = Set.of("active", "paused", "finished");


    @Override
    public ListingSearchCriteria buildSearchCriteria(
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final List<String> price,
            final List<String> rating,
            final String from,
            final String until,
            final int page,
            final String sort,
            final User viewer,
            final List<Long> neighborhoodIds) {
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
        final List<String> ratingBands = collectRatingBandParams(rating);
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
        final LocalDate browseWallDate = publicBrowseMinBookableWallDate();
        final List<Long> mergedNeighborhoodIds = mergeNeighborhoodIdsForSearch(query, neighborhoodIds);
        return ListingSearchCriteria.builder()
                .query(query)
                .transmissions(transmissions)
                .powertrains(powertrains)
                .carTypes(mergedCarTypes)
                .priceBands(bands)
                .ratingBands(ratingBands)
                .availabilityRange(rangeStart, rangeEndExclusive)
                .page(page)
                .uiPageSize(paginationPolicy.getUiPageSize())
                .dbFetchSize(paginationPolicy.getDbFetchSize())
                .sortBy(sortBy)
                .sortDirection(sortDir)
                .browseWallDate(browseWallDate)
                .excludeOwnerUserId(browseExcludeOwnerId(viewer))
                .neighborhoodIds(mergedNeighborhoodIds)
                .build();
    }

    private List<Long> mergeNeighborhoodIdsForSearch(final String query, final List<Long> explicitNeighborhoodIds) {
        final LinkedHashSet<Long> merged = new LinkedHashSet<>();
        if (explicitNeighborhoodIds != null) {
            for (final Long id : explicitNeighborhoodIds) {
                if (id != null && id > 0L && locationService.findNeighborhoodById(id).isPresent()) {
                    merged.add(id);
                }
            }
        }
        final String q = query != null ? query.trim() : "";
        if (!q.isEmpty()) {
            merged.addAll(NeighborhoodNameMatcher.idsMatchingFuzzyTokens(
                    q,
                    locationService.findAllNeighborhoods(),
                    2,
                    3));
        }
        return List.copyOf(merged);
    }

    private static Long browseExcludeOwnerId(final User viewer) {
        return viewer != null ? viewer.getId() : null;
    }

    /**
     * First wall-calendar day that can still intersect rider-visible availability given
     * {@code app.reservation.pickup-lead-hours} (same horizon as {@code ReservationServiceImpl}).
     */
    private LocalDate publicBrowseMinBookableWallDate() {
        return LocalDate.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS),
                AvailabilityPeriod.WALL_ZONE);
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
        final Instant minPickupExclusive =
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS);
        final Map<Long, LocalTime> checkInByListing = listingDao.findCheckInTimeByListingIds(listingIds);
        final ZoneId wallZone = AvailabilityPeriod.WALL_ZONE;
        for (final Map.Entry<Long, TreeSet<LocalDate>> e : daysByListing.entrySet()) {
            final LocalTime checkIn = checkInByListing.getOrDefault(e.getKey(), Listing.DEFAULT_CHECK_IN_TIME);
            e.getValue().removeIf(day ->
                    !ZonedDateTime.of(day, checkIn, wallZone).toInstant().isAfter(minPickupExclusive));
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

    private static final Set<String> RATING_BANDS = Set.of("UNDER_2", "2_TO_3", "3_TO_4", "OVER_4");

    private static List<String> collectRatingBandParams(final List<String> raw) {
        final List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            if (RATING_BANDS.contains(u)) {
                out.add(u);
            }
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public String formatPublicDeliveryLocation(final Listing listing) {
        return formatPublicPickupLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatFullDeliveryLocation(final Listing listing) {
        return formatFullPickupLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatPublicPickupLocation(final Listing listing) {
        final String street = listing.getStartPointStreet() == null ? "" : listing.getStartPointStreet().trim();
        if (listing.getNeighborhoodId().isEmpty()) {
            return street;
        }
        final String neighborhoodName = locationService.findNeighborhoodById(listing.getNeighborhoodId().get())
                .map(Neighborhood::getName)
                .orElse("");
        if (neighborhoodName.isBlank()) {
            return street;
        }
        if (street.isBlank()) {
            return neighborhoodName.trim();
        }
        return street + ", " + neighborhoodName.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public String formatFullPickupLocation(final Listing listing) {
        final String street = listing.getStartPointStreet() == null ? "" : listing.getStartPointStreet().trim();
        final Optional<String> numberOpt = listing.getStartPointNumber();
        final String streetWithNumber;
        if (numberOpt.isPresent() && !numberOpt.get().isBlank()) {
            streetWithNumber = street.isBlank() ? numberOpt.get().trim() : street + " " + numberOpt.get().trim();
        } else {
            streetWithNumber = street;
        }
        if (listing.getNeighborhoodId().isEmpty()) {
            return streetWithNumber;
        }
        final String neighborhoodName = locationService.findNeighborhoodById(listing.getNeighborhoodId().get())
                .map(Neighborhood::getName)
                .orElse("");
        if (neighborhoodName.isBlank()) {
            return streetWithNumber;
        }
        if (streetWithNumber.isBlank()) {
            return neighborhoodName.trim();
        }
        return streetWithNumber + ", " + neighborhoodName.trim();
    }

    private static boolean riderSeesSensitiveAddressNumbers(final Reservation reservation) {
        return reservation.getPaymentReceiptFileId().isPresent() || reservation.isPaymentApproved();
    }

    @Override
    @Transactional(readOnly = true)
    public String formatDeliveryForReservationView(
            final Listing listing,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullDeliveryLocation(listing);
        }
        return formatPublicDeliveryLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatPickupForReservationView(
            final Listing listing,
            final Reservation reservation,
            final boolean viewerIsOwner) {
        if (viewerIsOwner || riderSeesSensitiveAddressNumbers(reservation)) {
            return formatFullPickupLocation(listing);
        }
        return formatPublicPickupLocation(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public String formatRiderReservationHandoverSummary(final Listing listing, final Reservation reservation) {
        final String p = formatPickupForReservationView(listing, reservation, false);
        final String d = formatDeliveryForReservationView(listing, reservation, false);
        if (p.isBlank()) {
            return d;
        }
        if (d.isBlank() || p.trim().equals(d.trim())) {
            return p;
        }
        return p + " · " + d;
    }

    @Override
    @Transactional(readOnly = true)
    public String formatOwnerReservationHandoverSummary(final Listing listing) {
        final String p = formatFullPickupLocation(listing);
        final String d = formatFullDeliveryLocation(listing);
        if (p.isBlank()) {
            return d;
        }
        if (d.isBlank() || p.trim().equals(d.trim())) {
            return p;
        }
        return p + " · " + d;
    }

    @Override
    @Transactional(readOnly = true)
    public void validatePublicationAvailabilityAgainstWallCalendar(final List<AvailabilityPeriod> periods) {
        listingAvailabilityPolicy.validateAvailabilityWithinPublishHorizon(
                LocalDate.now(AvailabilityPeriod.WALL_ZONE), periods);
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredMaxAvailabilityForwardWallDays() {
        return listingAvailabilityPolicy.getMaxAvailabilityForwardWallDays();
    }

    @Override
    @Transactional(readOnly = true)
    public int getConfiguredPickupLeadHours() {
        return reservationTimingPolicy.getPickupLeadHours();
    }

    @Override
    @Transactional
    public void pauseActiveListingsDueToMissingCbuForOwnerAndNotify(final long ownerId) {
        final List<Listing> active = listingDao.findListingsByOwnerIdAndStatus(ownerId, Listing.Status.ACTIVE);
        if (active.isEmpty()) {
            return;
        }
        final User owner = userService.getUserById(ownerId).orElse(null);
        if (owner == null) {
            return;
        }
        final String ownerEmail = owner.getEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return;
        }
        final String ownerFullName = owner.getForename() + " " + owner.getSurname();
        final Locale ownerMailLocale = userService.resolveMailLocale(ownerId);
        for (final Listing li : active) {
            if (!listingDao.updateListingStatus(li.getId(), Listing.Status.PAUSED_DUE_TO_LACK_OF_CBU, Listing.Status.ACTIVE)) {
                continue;
            }
            emailService.sendListingPausedDueToMissingCbu(ListingPausedMissingCbuOwnerEmailPayload.builder()
                    .messageLocale(ownerMailLocale)
                    .ownerEmail(ownerEmail)
                    .ownerFullName(ownerFullName)
                    .vehicleLabel(li.getTitle())
                    .listingId(li.getId())
                    .build());
        }
    }

    @Override
    @Transactional
    public void resumeListingsPausedDueToMissingCbuForOwner(final long ownerId) {
        final List<Listing> toResume = listingDao.findListingsByOwnerIdAndStatus(
                ownerId, Listing.Status.PAUSED_DUE_TO_LACK_OF_CBU);
        for (final Listing li : toResume) {
            listingDao.updateListingStatus(li.getId(), Listing.Status.ACTIVE, Listing.Status.PAUSED_DUE_TO_LACK_OF_CBU);
        }
    }
}
