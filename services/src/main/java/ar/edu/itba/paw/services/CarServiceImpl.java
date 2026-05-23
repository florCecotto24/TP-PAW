package ar.edu.itba.paw.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.ImageUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.listing.DuplicatePlateException;
import ar.edu.itba.paw.exception.listing.ListingValidationException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.Listing;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.CarCard;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.email.ListingPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.util.CbuRules;
import ar.edu.itba.paw.models.util.ListingSearchCriteria;
import ar.edu.itba.paw.models.util.OwnerListingSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;
import ar.edu.itba.paw.persistence.ListingDao;
import ar.edu.itba.paw.services.policy.ReservationTimingPolicy;

@Service
public final class CarServiceImpl implements CarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarServiceImpl.class);

    private final CarDao carDao;
    private final ListingDao listingDao;
    private final ImageService imageService;
    private final CarPictureService carPictureService;
    private final UserService userService;
    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final ReservationTimingPolicy reservationTimingPolicy;
    private final ListingAvailabilityService listingAvailabilityService;
    private final EmailService emailService;
    private final ListingService listingService;

    @Autowired
    public CarServiceImpl(
            final CarDao carDao,
            final ListingDao listingDao,
            final ImageService imageService,
            final CarPictureService carPictureService,
            final UserService userService,
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final ReservationTimingPolicy reservationTimingPolicy,
            @Lazy final ListingAvailabilityService listingAvailabilityService,
            final EmailService emailService,
            @Lazy final ListingService listingService) {
        this.carDao = carDao;
        this.listingDao = listingDao;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
        this.userService = userService;
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.reservationTimingPolicy = reservationTimingPolicy;
        this.listingAvailabilityService = listingAvailabilityService;
        this.emailService = emailService;
        this.listingService = listingService;
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
    @Deprecated
    public Car createCar(
            final long ownerId,
            final String plate,
            final String brand,
            final String model,
            final Car.Type type,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission) {
        final long carModelId = carBrandService.findOrCreateUnvalidated(brand)
                .flatMap(b -> carModelService.findOrCreateUnvalidated(b.getId(), model, type))
                .map(m -> m.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot resolve car model for brand=" + brand + ", model=" + model));
        return createCar(ownerId, plate, carModelId, type, powertrain, transmission);
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
            final List<ImageUpload> images) {
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
    public List<Car> getCheapestCars() {
        return carDao.getCheapestCars();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> getMostRecentCars() {
        return carDao.getMostRecentCars();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getOwnerCarCards(final OwnerListingSearchCriteria criteria) {
        return carDao.getOwnerCarCards(criteria);
    }

    @Override
    @Transactional
    public Car.Status toggleCarStatus(final long ownerId, final long carId) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new ListingValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        final Car car = carOpt.get();
        final Car.Status current = car.getStatus();
        final Car.Status newCarStatus;
        final Listing.Status newListingStatus;
        if (current == Car.Status.ACTIVE) {
            newCarStatus = Car.Status.PAUSED;
            newListingStatus = Listing.Status.PAUSED;
        } else if (current == Car.Status.PAUSED) {
            final Optional<User> ownerRow = userService.getUserById(ownerId);
            if (ownerRow.isEmpty() || !userService.hasValidCbu(ownerRow.get())) {
                throw new ListingValidationException(
                        MessageKeys.LISTING_ACTIVATE_CBU_REQUIRED, CbuRules.REQUIRED_DIGIT_LENGTH);
            }
            newCarStatus = Car.Status.ACTIVE;
            newListingStatus = Listing.Status.ACTIVE;
        } else {
            throw new ListingValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
        carDao.setCarStatus(carId, newCarStatus);
        listingDao.findActiveOrPausedListingByCar(carId)
                .ifPresent(l -> l.setStatus(newListingStatus));
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
        listingDao.findActiveOrPausedListingByCar(carId)
                .ifPresent(l -> l.setStatus(Listing.Status.FINISHED));
        return true;
    }

    @Override
    @Transactional
    public void setCarLackDoc(final long carId) {
        carDao.setCarStatus(carId, Car.Status.LACK_DOC);
    }

    @Override
    @Transactional
    public void clearCarLackDoc(final long carId) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isPresent() && carOpt.get().getStatus() == Car.Status.LACK_DOC) {
            carDao.setCarStatus(carId, Car.Status.ACTIVE);
        }
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

    /**
     * Transitional implementation: delegates to {@link ListingDao} for browse pagination
     * and projects each {@code ListingCard} into a {@link CarCard}. This will be replaced by a
     * pure car-centric DAO query when the {@code listings} table is dropped.
     */
    private Page<CarCard> browseCarCardsPage(
            final int page, final int pageSize, final User viewer, final boolean cheapest) {
        final LocalDate wall = LocalDate.ofInstant(
                Instant.now().plus(reservationTimingPolicy.getPickupLeadHours(), ChronoUnit.HOURS),
                AvailabilityPeriod.WALL_ZONE);
        final Long excludeOwnerId = viewer != null ? viewer.getId() : null;
        final int offset = page * pageSize;
        final List<ar.edu.itba.paw.models.dto.ListingCard> rows = cheapest
                ? listingDao.getCheapestListingCardsWindow(offset, pageSize, wall, excludeOwnerId)
                : listingDao.getMostRecentListingCardsWindow(offset, pageSize, wall, excludeOwnerId);
        final long total = listingDao.countBrowseEligibleActiveListings(wall, excludeOwnerId);
        return new Page<>(toCarCards(rows), page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> searchCarCards(final ListingSearchCriteria criteria) {
        final Page<ar.edu.itba.paw.models.dto.ListingCard> raw = listingDao.searchListingCards(criteria);
        return new Page<>(toCarCards(raw.getContent()), raw.getCurrentPage(), raw.getPageSize(), raw.getTotalItems());
    }

    @Override
    @Transactional(readOnly = true)
    public ListingSearchCriteria buildSearchCriteria(
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
        // Delegate to ListingService temporarily; the criteria builder is non-trivial.
        // After ListingService is removed this method will inline the implementation.
        return listingService.buildSearchCriteria(
                query, category, transmission, powertrain, priceMin, priceMax, rating,
                from, until, page, sort, viewer, neighborhoodIds);
    }

    @Override
    @Transactional(readOnly = true)
    public OwnerListingSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<String> category,
            final List<String> transmission,
            final List<String> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> listingStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final String sort) {
        return listingService.buildOwnerListingSearchCriteria(
                ownerId, category, transmission, powertrain, priceMin, priceMax,
                listingStatus, rating, textQuery, page, sort);
    }

    @Override
    @Transactional
    public void refreshExhaustedCarsToFinished() {
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
    public void pauseActiveCarsDueToMissingCbuForOwnerAndNotify(final long ownerId) {
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
                    .listingId(car.getId())
                    .build());
        }
    }

    @Override
    @Transactional
    public void resumeCarsPausedDueToMissingCbuForOwner(final long ownerId) {
        final List<Car> toResume = carDao.findCarsByOwnerAndStatuses(ownerId, List.of(Car.Status.LACK_DOC));
        for (final Car car : toResume) {
            carDao.updateCarStatusIfCurrent(car.getId(), Car.Status.ACTIVE, Car.Status.LACK_DOC);
        }
    }

    private static String carLabel(final Car car) {
        final String brand = car.getBrand() != null ? car.getBrand() : "";
        final String model = car.getModel() != null ? car.getModel() : "";
        final String sep = !brand.isEmpty() && !model.isEmpty() ? " " : "";
        return brand + sep + model;
    }

    private List<CarCard> toCarCards(final List<ar.edu.itba.paw.models.dto.ListingCard> rows) {
        return rows.stream()
                .map(lc -> new CarCard(
                        lc.getCarId(),
                        lc.getBrand(),
                        lc.getModel(),
                        lc.getImageId(),
                        lc.getListingId(),
                        lc.getDayPrice(),
                        Car.Status.ACTIVE,
                        lc.getRatingAvg().orElse(null)))
                .collect(java.util.stream.Collectors.toList());
    }
}
