package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.Image;
import ar.edu.itba.paw.models.domain.StoredFile;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.car.OwnerCarDetailPageModel;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.email.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.models.util.media.CarGalleryMediaContentTypes;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;
import ar.edu.itba.paw.persistence.CarDao;

import ar.edu.itba.paw.services.car.view.OwnerCarDetailViewService;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.ImageService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.UserService;
@Service
public final class CarServiceImpl implements CarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarServiceImpl.class);

    private final CarDao carDao;
    private final ImageService imageService;
    private final CarPictureService carPictureService;
    private final UserService userService;
    private final CarAvailabilityService carAvailabilityService;
    private final EmailService emailService;
    private final StoredFileService storedFileService;
    private final CarSearchService carSearchService;
    private final OwnerCarDetailViewService ownerCarDetailViewService;

    @Autowired
    public CarServiceImpl(
            final CarDao carDao,
            final ImageService imageService,
            final CarPictureService carPictureService,
            final UserService userService,
            @Lazy final CarAvailabilityService carAvailabilityService,
            final EmailService emailService,
            final StoredFileService storedFileService,
            @Lazy final CarSearchService carSearchService,
            @Lazy final OwnerCarDetailViewService ownerCarDetailViewService) {
        this.carDao = carDao;
        this.imageService = imageService;
        this.carPictureService = carPictureService;
        this.userService = userService;
        this.carAvailabilityService = carAvailabilityService;
        this.emailService = emailService;
        this.storedFileService = storedFileService;
        this.carSearchService = carSearchService;
        this.ownerCarDetailViewService = ownerCarDetailViewService;
    }

    @Override
    @Transactional
    public Car createCar(
            final long ownerId,
            final String plate,
            final long carModelId,
            final Integer year,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission) {
        return carDao.createCar(ownerId, plate, carModelId, year, powertrain, transmission);
    }

    @Override
    @Transactional
    public Car publishCar(
            final long ownerId,
            final String plate,
            final long carModelId,
            final Integer year,
            final Car.Powertrain powertrain,
            final Car.Transmission transmission,
            final String description,
            final List<GalleryMediaUpload> galleryMedia,
            final String insuranceFilename,
            final String insuranceContentType,
            final byte[] insuranceData) {
        // Defense-in-depth: the UI hides the publish entry point and the controller redirects blocked
        // owners away from GET /publish-car. A direct POST still needs the same guard because the form
        // could be replayed from a cached page or hit via cURL.
        requireOwnerNotBlocked(ownerId);
        if (carDao.existsByOwnerAndPlate(ownerId, plate)) {
            throw new DuplicatePlateException(plate);
        }
        final Car car = carDao.createCar(ownerId, plate, carModelId, year, powertrain, transmission);
        if (description != null && !description.isBlank()) {
            car.setDescription(description.strip());
        }
        int displayOrder = 1;
        if (galleryMedia != null) {
            for (final GalleryMediaUpload media : galleryMedia) {
                if (media.getData() == null || media.getData().length == 0) {
                    continue;
                }
                if (CarGalleryMediaContentTypes.isImageContentType(media.getContentType())) {
                    final Image image = imageService.createImage(
                            media.getFilename(), media.getContentType(), media.getData());
                    carPictureService.createCarPicture(car.getId(), image.getId(), displayOrder);
                } else if (CarGalleryMediaContentTypes.isVideoContentType(
                        media.getContentType(), media.getFilename())) {
                    final StoredFile video = storedFileService.create(
                            ownerId,
                            media.getFilename() != null ? media.getFilename() : "video",
                            media.getContentType() != null ? media.getContentType() : "video/mp4",
                            media.getData());
                    carPictureService.createCarPictureFromVideo(car.getId(), video.getId(), displayOrder);
                } else {
                    throw new CarValidationException(MessageKeys.CAR_GALLERY_MEDIA_INVALID_TYPE);
                }
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
                        MessageKeys.CAR_ACTIVATE_CBU_REQUIRED, CbuRules.REQUIRED_DIGIT_LENGTH);
            }
            // Blocked owners (e.g. unpaid refund proofs) cannot resume their own listings.
            if (ownerRow.get().isBlocked()) {
                throw new CarValidationException(MessageKeys.CAR_ACTIVATE_OWNER_BLOCKED);
            }
            // Defense-in-depth: PAUSED should imply documentation is present (clearCarInsuranceDocument
            // demotes ACTIVE/PAUSED to LACK_DOC when insurance is removed), but mirror the explicit
            // check in resumeCarsForRestoredCbu so we never let an undocumented car reach ACTIVE.
            if (car.getInsuranceFileId().isEmpty()) {
                throw new CarValidationException(MessageKeys.CAR_INSURANCE_INVALID);
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
        return carDao.findSimilarCarCards(carId, limit, carSearchService.publicBrowseMinBookableWallDate(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getCheapestCarCards(final int page, final int pageSize) {
        // Home browse intentionally does not exclude the viewer's own cars: owners want to see
        // how their own listings render alongside the rest of the catalog.
        return carDao.getCheapestCarCards(page, pageSize, carSearchService.publicBrowseMinBookableWallDate(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getMostRecentCarCards(final int page, final int pageSize) {
        return carDao.getMostRecentCarCards(page, pageSize, carSearchService.publicBrowseMinBookableWallDate(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> searchCarCards(final CarSearchCriteria criteria) {
        return carDao.searchCarCards(criteria);
    }

    @Override
    public CarSearchCriteria buildSearchCriteria(
            final String query,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<String> rating,
            final String from,
            final String until,
            final int page,
            final int uiPageSize,
            final String sort,
            final User viewer,
            final List<Long> neighborhoodIds,
            final boolean flexible,
            final String flexMonth,
            final Integer flexDays) {
        return carSearchService.buildSearchCriteria(
                query, category, transmission, powertrain,
                priceMin, priceMax, rating, from, until, page, uiPageSize, sort,
                viewer, neighborhoodIds, flexible, flexMonth, flexDays);
    }

    @Override
    public OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<Car.Status> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final int pageSize,
            final String sort) {
        return carSearchService.buildOwnerCarSearchCriteria(
                ownerId, category, transmission, powertrain, priceMin, priceMax,
                carStatus, rating, textQuery, page, pageSize, sort);
    }

    @Override
    public OwnerCarSearchCriteria buildOwnerCarSearchCriteria(
            final long ownerId,
            final List<Car.Type> category,
            final List<Car.Transmission> transmission,
            final List<Car.Powertrain> powertrain,
            final BigDecimal priceMin,
            final BigDecimal priceMax,
            final List<Car.Status> carStatus,
            final List<String> rating,
            final String textQuery,
            final int page,
            final int pageSize,
            final String sort,
            final Long excludeCarId) {
        return carSearchService.buildOwnerCarSearchCriteria(
                ownerId, category, transmission, powertrain, priceMin, priceMax,
                carStatus, rating, textQuery, page, pageSize, sort, excludeCarId);
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
                    carAvailabilityService.getBookableWallAvailabilityPeriodsByCar(carId);
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
            emailService.sendListingPausedDueToMissingCbu(CarPausedMissingCbuOwnerEmailPayload.builder()
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
            // Only re-activate when the insurance document is also present; if the car was in
            // LACK_DOC because of missing insurance (not just missing CBU), restoring the CBU
            // alone is not enough to make it active.
            if (car.getInsuranceFileId().isPresent()) {
                carDao.updateCarStatusIfCurrent(car.getId(), Car.Status.ACTIVE, Car.Status.LACK_DOC);
            }
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
        // Defense-in-depth: insurance upload can promote a LACK_DOC car back to ACTIVE, so a blocked
        // owner could re-introduce a bookable car this way. Reject before touching storage.
        requireOwnerNotBlocked(ownerId);
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        if (data == null || data.length == 0) {
            throw new CarValidationException(MessageKeys.CAR_INSURANCE_INVALID);
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

    @Override
    @Transactional(readOnly = true)
    public boolean isModelPendingValidation(final long carId) {
        return carDao.getCarById(carId)
                .map(Car::isModelPendingValidation)
                .orElse(false);
    }

    @Override
    @Transactional
    public void updateMinimumRentalDays(final long carId, final int days) {
        carDao.updateMinimumRentalDays(carId, days);
    }

    @Override
    @Transactional
    public void updateRatingAvg(final long carId, final java.math.BigDecimal average) {
        carDao.updateRatingAvg(carId, average);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> findAdminPausedCars() {
        return carDao.findCarsByStatus(Car.Status.ADMIN_PAUSED);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Car> findAllCarsPaginated(final int page, final int pageSize) {
        return carDao.findAllCarsPaginated(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> findCarsByModelId(final long modelId) {
        return carDao.findCarsByModelId(modelId);
    }

    @Override
    @Transactional
    public void markCarAsAdminPaused(final long carId) {
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        if (car.getStatus() == Car.Status.DEACTIVATED) {
            throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
        car.setStatus(Car.Status.ADMIN_PAUSED);
    }

    @Override
    @Transactional
    public void releaseAdminCarPause(final long carId) {
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        if (car.getStatus() != Car.Status.ADMIN_PAUSED) {
            throw new IllegalStateException("Car is not admin-paused: " + carId);
        }
        // markCarAsAdminPaused accepts every non-DEACTIVATED status (including LACK_DOC), so we cannot
        // assume the car had valid documentation when it was paused. The Car.Status javadoc reserves
        // ACTIVE for listings with all required documents, so re-check insurance and the owner's CBU
        // before promoting; otherwise drop back to LACK_DOC and let the regular re-activation path
        // (insurance upload / CBU restored) bring the car back online once the owner completes them.
        final boolean hasInsurance = car.getInsuranceFileId().isPresent();
        final boolean hasValidCbu = userService.getUserById(car.getOwnerId())
                .map(userService::hasValidCbu)
                .orElse(false);
        if (hasInsurance && hasValidCbu) {
            car.setStatus(Car.Status.ACTIVE);
        } else {
            car.setStatus(Car.Status.LACK_DOC);
        }
    }

    @Override
    @Transactional
    public void clearCarModel(final long carId) {
        carDao.getCarById(carId).ifPresent(car -> car.setCarModel(null));
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
        return normalizedBrandModel(car.getBrand(), car.getModel())
                .flatMap(bm -> carDao.findActiveDayPriceMarketInsightByBrandAndModel(
                        bm[0], bm[1], excludeCarId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ConsumerCarCardMarketContext> resolveConsumerPriceMarketContexts(
            final List<CarCard> cards) {
        if (cards == null || cards.isEmpty()) {
            return Map.of();
        }
        final Map<String, Optional<CarPriceMarketInsight>> insightCache = new HashMap<>();
        final Map<Long, ConsumerCarCardMarketContext> contexts = new HashMap<>();
        for (final CarCard card : cards) {
            normalizedBrandModel(card.getBrand(), card.getModel())
                    .flatMap(bm -> {
                        final String cacheKey = bm[0] + "|" + bm[1] + "|" + card.getCarId();
                        final Optional<CarPriceMarketInsight> insight = insightCache.computeIfAbsent(
                                cacheKey,
                                k -> carDao.findActiveDayPriceMarketInsightByBrandAndModel(
                                        bm[0], bm[1], card.getCarId()));
                        return ConsumerCarCardMarketContext.fromInsight(insight, card.getDayPrice());
                    })
                    .ifPresent(ctx -> contexts.put(card.getCarId(), ctx));
        }
        return contexts;
    }

    private static Optional<String[]> normalizedBrandModel(final String brand, final String model) {
        if (brand == null || brand.isBlank() || model == null || model.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new String[] {brand.trim(), model.trim()});
    }

    @Override
    public Optional<OwnerCarDetailPageModel> buildOwnerCarDetailPageModel(
            final long carId, final Locale locale) {
        return ownerCarDetailViewService.buildOwnerCarDetailPageModel(carId, locale);
    }

    /**
     * Throws {@link CarValidationException} with {@link MessageKeys#CAR_MUTATION_OWNER_BLOCKED} when
     * {@code ownerId} is currently blocked (e.g. by the overdue-refund-proof sweep). Used as a guard
     * by every owner-side mutation that could re-introduce a bookable car/listing. Reads through
     * {@link UserService} (not {@code UserDao}) to respect the "service may only call its own DAO" rule.
     */
    private void requireOwnerNotBlocked(final long ownerId) {
        final Optional<User> ownerOpt = userService.getUserById(ownerId);
        if (ownerOpt.isPresent() && ownerOpt.get().isBlocked()) {
            throw new CarValidationException(MessageKeys.CAR_MUTATION_OWNER_BLOCKED);
        }
    }
}
