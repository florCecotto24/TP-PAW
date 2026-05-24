package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.ListingAvailability;
import ar.edu.itba.paw.models.domain.Neighborhood;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.email.ListingPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.util.ArsMoneyFormat;
import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.models.util.CarSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerCarSearchCriteria;
import ar.edu.itba.paw.models.util.WallDateTimeDisplayFormat;
import ar.edu.itba.paw.models.util.WallDateTimeParsing;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.services.policy.ListingAvailabilityPolicy;
import ar.edu.itba.paw.services.policy.PaginationPolicy;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;
import ar.edu.itba.paw.services.util.NeighborhoodNameMatcher;

@Service
public final class CarServiceImpl implements CarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarServiceImpl.class);

    private final CarDao carDao;
    private final ImageService imageService;
    private final CarPictureService carPictureService;
    private final UserService userService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ListingAvailabilityService listingAvailabilityService;
    private final EmailService emailService;
    private final ReservationService reservationService;
    private final LocationService locationService;
    private final ListingAvailabilityPolicy listingAvailabilityPolicy;
    private final PaginationPolicy paginationPolicy;
    private final StoredFileService storedFileService;

    @Autowired
    public CarServiceImpl(
            final CarDao carDao,
            final ImageService imageService,
            final CarPictureService carPictureService,
            final UserService userService,
            final ReservationTimingPolicy reservationTimingPolicy,
            @Lazy final ListingAvailabilityService listingAvailabilityService,
            final EmailService emailService,
            @Lazy final ReservationService reservationService,
            final LocationService locationService,
            final ListingAvailabilityPolicy listingAvailabilityPolicy,
            final PaginationPolicy paginationPolicy,
            final StoredFileService storedFileService) {
        this.carDao = carDao;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
        this.userService = userService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.listingAvailabilityService = listingAvailabilityService;
        this.emailService = emailService;
        this.reservationService = reservationService;
        this.locationService = locationService;
        this.listingAvailabilityPolicy = listingAvailabilityPolicy;
        this.paginationPolicy = paginationPolicy;
        this.storedFileService = storedFileService;
    }

    @Override
    @Transactional
    public Car createCar(
            final long ownerId,
            final String plate,
            final long carModelId,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission) {
        return carDao.createCar(ownerId, plate, carModelId, type, powertrain, transmission);
    }

    @Override
    @Transactional
    public Car publishCar(
            final long ownerId,
            final String plate,
            final long carModelId,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission,
            final List<ImageUpload> images,
            final String insuranceFilename,
            final String insuranceContentType,
            final byte[] insuranceData) {
        if (carDao.existsByOwnerAndPlate(ownerId, plate)) {
            throw new DuplicatePlateException(plate);
        }
        final Car car = carDao.createCar(ownerId, plate, carModelId, type, powertrain, transmission);
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
        if (insuranceData != null && insuranceData.length > 0) {
            final StoredFile stored = storedFileService.create(
                    ownerId,
                    insuranceFilename != null ? insuranceFilename : "insurance",
                    insuranceContentType != null ? insuranceContentType : "application/octet-stream",
                    insuranceData);
            carDao.updateInsuranceDocument(car.getId(), stored.getId());
        } else {
            carDao.setCarStatus(car.getId(), Car.Status.LACK_DOC);
        }
        return car;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOwnerAndPlate(final long ownerId, final String plate) {
        return carDao.existsByOwnerAndPlate(ownerId, plate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Car> getCarById(final long id) {
        return carDao.getCarById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getOwnerCarCards(final OwnerCarSearchCriteria criteria) {
        return carDao.getOwnerCarCards(criteria);
    }

    @Override
    @Transactional
    public Car.Status toggleCarStatus(final long ownerId, final long carId) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        final Car car = carOpt.get();
        final Car.Status current = car.getStatus();
        final Car.Status newCarStatus;
        if (current == Car.Status.ACTIVE) {
            newCarStatus = Car.Status.PAUSED;
        } else if (current == Car.Status.PAUSED) {
            final Optional<User> ownerRow = userService.getUserById(ownerId);
            if (ownerRow.isEmpty() || !userService.hasValidCbu(ownerRow.get())) {
                throw new CarValidationException(
                        MessageKeys.LISTING_ACTIVATE_CBU_REQUIRED, CbuRules.REQUIRED_DIGIT_LENGTH);
            }
            newCarStatus = Car.Status.ACTIVE;
        } else {
            throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
        carDao.setCarStatus(carId, newCarStatus);
        return newCarStatus;
    }

    @Override
    @Transactional
    public boolean deactivateCar(final long ownerId, final long carId) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            return false;
        }
        carDao.setCarStatus(carId, Car.Status.DEACTIVATED);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarCard> findSimilarCarCards(final long carId, final int limit, final User viewer) {
        if (limit <= 0) {
            return List.of();
        }
        final LocalDate wall = LocalDate.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS),
                AvailabilityPeriod.WALL_ZONE);
        final Long excludeOwnerId = viewer != null ? viewer.getId() : null;
        return carDao.findSimilarCarCards(carId, limit, wall, excludeOwnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getCheapestCarCards(final int page, final int pageSize, final User viewer) {
        return browseCarCardsPage(page, pageSize, viewer, /*cheapest*/ true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getMostRecentCarCards(final int page, final int pageSize, final User viewer) {
        return browseCarCardsPage(page, pageSize, viewer, /*cheapest*/ false);
    }

    private Page<CarCard> browseCarCardsPage(
            final int page, final int pageSize, final User viewer, final boolean cheapest) {
        final LocalDate wall = LocalDate.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS),
                AvailabilityPeriod.WALL_ZONE);
        final Long excludeOwnerId = viewer != null ? viewer.getId() : null;
        final int offset = page * pageSize;
        final List<CarCard> rows = cheapest
                ? carDao.getCheapestCarCardsWindow(offset, pageSize, wall, excludeOwnerId)
                : carDao.getMostRecentCarCardsWindow(offset, pageSize, wall, excludeOwnerId);
        final long total = carDao.countBrowseEligibleActiveCars(wall, excludeOwnerId);
        return new Page<>(rows, page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> searchCarCards(final CarSearchCriteria criteria) {
        return carDao.searchCarCards(criteria);
    }

    @Override
    @Transactional(readOnly = true)
    public CarSearchCriteria buildSearchCriteria(
            final String query,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
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
        final BigDecimal minPrice = priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0 ? priceMin : null;
        final BigDecimal maxPrice = priceMax != null && priceMax.compareTo(BigDecimal.ZERO) >= 0 ? priceMax : null;
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
        final String sortBy = sortParts.length > 0 ? sortParts[0].trim() : "date";
        final String sortDir = sortParts.length > 1 ? sortParts[1].trim() : "desc";
        final LocalDate browseWallDate = publicBrowseMinBookableWallDate();
        final List<Long> mergedNeighborhoodIds = mergeNeighborhoodIdsForSearch(query, neighborhoodIds);
        return CarSearchCriteria.builder()
                .query(query)
                .transmissions(transmissions)
                .powertrains(powertrains)
                .carTypes(mergedCarTypes)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .ratingBands(ratingBands)
                .availabilityRange(rangeStart, rangeEndExclusive)
                .page(page)
                .uiPageSize(paginationPolicy.getUiPageSize())
                .dbFetchSize(paginationPolicy.getDbFetchSize())
                .sortBy(sortBy)
                .sortDirection(sortDir)
                .browseWallDate(browseWallDate)
                .excludeOwnerUserId(viewer != null ? viewer.getId() : null)
                .neighborhoodIds(mergedNeighborhoodIds)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final String sort) {
        return buildOwnerCarSearchCriteria(
                ownerId, category, transmission, powertrain, priceMin, priceMax,
                carStatus, rating, textQuery, page, sort, 0, null);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final String sort,
            final int pageSizeOrZero,
            final Long excludeCarId) {
        final List<String> carTypes = collectCarTypeParams(category);
        final List<String> transmissions = collectTransmissionParams(transmission);
        final List<String> powertrains = collectPowertrainParams(powertrain);
        final BigDecimal minPrice = priceMin != null && priceMin.compareTo(BigDecimal.ZERO) >= 0 ? priceMin : null;
        final BigDecimal maxPrice = priceMax != null && priceMax.compareTo(BigDecimal.ZERO) >= 0 ? priceMax : null;
        final List<String> statuses = new ArrayList<>();
        if (carStatus != null) {
            for (final String s : carStatus) {
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
        final int pageSize =
                pageSizeOrZero > 0 ? pageSizeOrZero : paginationPolicy.getDefaultPageSize();
        return new OwnerCarSearchCriteria(
                ownerId,
                page,
                pageSize,
                statuses,
                textQuery,
                carTypes,
                transmissions,
                powertrains,
                minPrice,
                maxPrice,
                ratingBands,
                sortBy,
                sortDir,
                excludeCarId);
    }

    @Override
    @Transactional
    public void refreshExhaustedCarsToPaused() {
        final List<Car> activeCars = carDao.findCarsByStatus(Car.Status.ACTIVE);
        if (activeCars.isEmpty()) {
            LOGGER.atInfo().log("Car exhaustion sweep: no active cars");
            return;
        }
        int paused = 0;
        for (final Car car : activeCars) {
            final long carId = car.getId();
            final List<AvailabilityPeriod> bookable =
                    listingAvailabilityService.getBookableWallAvailabilityPeriodsByCar(carId);
            if (bookable.isEmpty()) {
                if (carDao.updateCarStatusIfCurrent(carId, Car.Status.PAUSED, Car.Status.ACTIVE)) {
                    paused++;
                }
            }
        }
        LOGGER.atInfo()
                .addArgument(paused)
                .addArgument(activeCars.size())
                .log("Car exhaustion sweep: paused {} of {} active cars");
    }

    @Override
    @Transactional
    public void pauseCarsForMissingCbu(final long ownerId) {
        final List<Car> active = carDao.findCarsByOwnerAndStatuses(
                ownerId, List.of(Car.Status.ACTIVE, Car.Status.PAUSED));
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
        for (final Car car : active) {
            final Car.Status current = car.getStatus();
            if (!carDao.updateCarStatusIfCurrent(car.getId(), Car.Status.LACK_DOC, current)) {
                continue;
            }
            final String label = carLabel(car);
            emailService.sendListingPausedDueToMissingCbu(ListingPausedMissingCbuOwnerEmailPayload.builder()
                    .messageLocale(ownerMailLocale)
                    .ownerEmail(ownerEmail)
                    .ownerFullName(ownerFullName)
                    .vehicleLabel(label)
                    .carId(car.getId())
                    .build());
        }
    }

    @Override
    @Transactional
    public void resumeCarsForRestoredCbu(final long ownerId) {
        final List<Car> toResume = carDao.findCarsByOwnerAndStatuses(ownerId, List.of(Car.Status.LACK_DOC));
        for (final Car car : toResume) {
            carDao.updateCarStatusIfCurrent(car.getId(), Car.Status.ACTIVE, Car.Status.LACK_DOC);
        }
    }

    @Override
    @Transactional
    public void uploadValidatedCarInsuranceDocument(
            final long ownerId,
            final long carId,
            final String originalFilename,
            final String contentType,
            final byte[] data) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        if (data == null || data.length == 0) {
            throw new CarValidationException(MessageKeys.PUBLISH_FAILED);
        }
        final Car car = carOpt.get();
        final StoredFile stored = storedFileService.create(
                ownerId,
                originalFilename != null ? originalFilename : "insurance",
                contentType != null ? contentType : "application/octet-stream",
                data);
        carDao.updateInsuranceDocument(carId, stored.getId());
        if (car.getStatus() == Car.Status.LACK_DOC) {
            final User owner = userService.getUserById(ownerId).orElse(null);
            if (owner != null && userService.hasValidCbu(owner)) {
                carDao.updateCarStatusIfCurrent(carId, Car.Status.ACTIVE, Car.Status.LACK_DOC);
            }
        }
    }

    @Override
    @Transactional
    public void clearCarInsuranceDocument(final long ownerId, final long carId) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        carDao.clearInsuranceDocument(carId);
        final Car car = carOpt.get();
        if (car.getStatus() == Car.Status.ACTIVE || car.getStatus() == Car.Status.PAUSED) {
            carDao.setCarStatus(carId, Car.Status.LACK_DOC);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUploadedInsurance(final long carId) {
        return carDao.getCarById(carId)
                .flatMap(Car::getInsuranceFileId)
                .isPresent();
    }

    private static String carLabel(final Car car) {
        final String brand = car.getBrand() != null ? car.getBrand() : "";
        final String model = car.getModel() != null ? car.getModel() : "";
        final String sep = !brand.isEmpty() && !model.isEmpty() ? " " : "";
        return brand + sep + model;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarPriceMarketInsight> getPriceMarketInsightForCar(
            final Car car, final Long excludeCarId) {
        if (car == null) {
            return Optional.empty();
        }
        final String brand = car.getBrand();
        final String model = car.getModel();
        if (brand == null || brand.isBlank() || model == null || model.isBlank()) {
            return Optional.empty();
        }
        return carDao.findActiveDayPriceMarketInsightByBrandAndModel(
                brand.trim(), model.trim(), excludeCarId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OwnerCarDetailPageModel> buildOwnerCarDetailPageModel(
            final long carId, final Locale locale) {
        return getCarById(carId).map(car -> {
            final List<ListingAvailability> availabilities = listingAvailabilityService.findByCarId(carId);
            final long carImageId = carPictureService.getCarPicturesByCarId(carId).stream()
                    .findFirst().map(p -> p.getImageId()).orElse(0L);
            final User owner = car.getOwner();
            final long ownerId = owner.getId();
            final List<Neighborhood> allNeighborhoods = locationService.findAllNeighborhoods();
            final ListingAvailability mostRecent = availabilities.stream()
                    .max(java.util.Comparator.comparing(ListingAvailability::getCreatedAt))
                    .orElse(null);
            final Long carNbId = mostRecent != null ? mostRecent.getNeighborhoodId().orElse(null) : null;
            final String carNeighborhoodName = carNbId == null
                    ? null
                    : allNeighborhoods.stream()
                            .filter(nb -> nb.getId() == carNbId)
                            .map(Neighborhood::getName)
                            .findFirst()
                            .orElse(null);
            final Map<String, Long> reservationStatusCounts =
                    reservationService.countCarReservationsByStatus(ownerId, carId);
            final long reservationTotal = reservationStatusCounts.values().stream().mapToLong(Long::longValue).sum();
            final String totalEarnings = ArsMoneyFormat.format(reservationService.getCarTotalEarnings(ownerId, carId));
            final String pendingEarnings = ArsMoneyFormat.format(reservationService.getCarPendingEarnings(ownerId, carId));
            final long totalDaysRented = reservationService.getCarTotalDaysRented(ownerId, carId);
            final long reservationsThisMonth = reservationService.getCarReservationsThisMonth(ownerId, carId);
            final long cancelled = reservationStatusCounts.getOrDefault("cancelled", 0L);
            final String cancellationRateDisplay = reservationTotal > 0
                    ? String.format("%.1f%%", (double) cancelled / reservationTotal * 100.0)
                    : "0.0%";
            final String nextReservationDisplay = reservationService.getCarNextReservationDate(ownerId, carId)
                    .map(dt -> WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(dt, locale))
                    .orElse(null);
            final int forwardDays = listingAvailabilityPolicy.getMaxAvailabilityForwardWallDays();
            final LocalDate wallToday = LocalDate.now(AvailabilityPeriod.WALL_ZONE);
            final List<ListingAvailability> editPastAvailabilities = availabilities.stream()
                    .filter(la -> la.getEndInclusive().isBefore(wallToday))
                    .collect(Collectors.toList());
            final String carCreatedAtDisplay = mostRecent != null
                    ? WallDateTimeDisplayFormat.formatUtcAsWallLocalNoSeconds(mostRecent.getCreatedAt(), locale)
                    : null;
            final String streetName = mostRecent != null ? mostRecent.getStartPointStreet() : null;
            final String streetNumber = mostRecent != null ? mostRecent.getStartPointNumber().orElse(null) : null;
            final BigDecimal dayPrice = mostRecent != null ? mostRecent.getDayPriceValue() : null;
            final LocalTime checkInTime = mostRecent != null ? mostRecent.getCheckInTime() : null;
            final LocalTime checkOutTime = mostRecent != null ? mostRecent.getCheckOutTime() : null;
            final String brand = car.getBrand() != null ? car.getBrand() : "";
            final String model = car.getModel() != null ? car.getModel() : "";
            final String title = brand + (!brand.isEmpty() && !model.isEmpty() ? " " : "") + model;
            final CarPriceMarketInsight priceMarketInsight =
                    getPriceMarketInsightForCar(car, car.getId()).orElse(null);
            return new OwnerCarDetailPageModel(
                    allNeighborhoods,
                    carNeighborhoodName,
                    streetNumber,
                    streetName,
                    dayPrice,
                    checkInTime,
                    checkOutTime,
                    title,
                    mostRecent != null,
                    carCreatedAtDisplay,
                    car,
                    owner,
                    availabilities,
                    carImageId,
                    car.getStatus().name(),
                    reservationStatusCounts,
                    reservationTotal,
                    totalEarnings,
                    pendingEarnings,
                    totalDaysRented,
                    reservationsThisMonth,
                    cancellationRateDisplay,
                    nextReservationDisplay,
                    editPastAvailabilities,
                    wallToday.plusDays(forwardDays).toString(),
                    wallToday,
                    priceMarketInsight);
        });
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

    private LocalDate publicBrowseMinBookableWallDate() {
        return LocalDate.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS),
                AvailabilityPeriod.WALL_ZONE);
    }

    private static final Set<String> LISTING_STATUSES = Set.of("active", "paused", "finished");
    private static final Set<String> RATING_BANDS = Set.of("UNDER_2", "2_TO_3", "3_TO_4", "OVER_4");

    private static List<String> collectCarTypeParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
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
            } catch (final IllegalArgumentException ex) {
                LOGGER.atDebug()
                        .setMessage("Ignoring invalid car type search filter token [{}]")
                        .addArgument(u)
                        .setCause(ex)
                        .log();
            }
        }
        return out;
    }

    private static List<String> collectTransmissionParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Transmission.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ex) {
                LOGGER.atDebug()
                        .setMessage("Ignoring invalid transmission search filter token [{}]")
                        .addArgument(u)
                        .setCause(ex)
                        .log();
            }
        }
        return out;
    }

    private static List<String> collectPowertrainParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
        for (final String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            final String u = s.trim().toUpperCase();
            try {
                Car.Powertrain.valueOf(u);
                out.add(u);
            } catch (final IllegalArgumentException ex) {
                LOGGER.atDebug()
                        .setMessage("Ignoring invalid powertrain search filter token [{}]")
                        .addArgument(u)
                        .setCause(ex)
                        .log();
            }
        }
        return out;
    }

    private static List<String> collectRatingBandParams(final List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
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

}
