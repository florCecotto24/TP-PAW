package ar.edu.itba.paw.services.car;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.GalleryMediaUpload;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.admin.AdminPromoterNotAdminException;
import ar.edu.itba.paw.exception.car.CarNotAdminPausedException;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.car.CarStatusTransitionConflictException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.car.DuplicatePlateException;
import ar.edu.itba.paw.models.domain.car.AvailabilityPeriod;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.file.StoredFile;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.car.CarCard;
import ar.edu.itba.paw.models.dto.car.CarModelPriceSample;
import ar.edu.itba.paw.models.dto.car.CarPriceMarketInsight;
import ar.edu.itba.paw.models.dto.car.ConsumerCarCardMarketContext;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingCbuOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedMissingIdentityOwnerEmailPayload;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.util.rules.CbuRules;
import ar.edu.itba.paw.models.util.search.CarSearchCriteria;
import ar.edu.itba.paw.models.util.search.CarSearchRequest;
import ar.edu.itba.paw.models.util.search.OwnerCarSearchCriteria;
import ar.edu.itba.paw.persistence.car.CarDao;

import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.file.StoredFileService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.services.user.UserLocaleService;
import ar.edu.itba.paw.services.user.UserReadinessService;
import ar.edu.itba.paw.services.user.UserService;
@Service
public class CarServiceImpl implements CarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarServiceImpl.class);

    private final CarDao carDao;
    private final UserService userService;
    private final UserReadinessService userReadinessService;
    private final UserLocaleService userLocaleService;
    private final CarAvailabilityService carAvailabilityService;
    private final EmailService emailService;
    private final StoredFileService storedFileService;
    private final CarSearchService carSearchService;
    private final AdminService adminService;
    private final CarListingPolicyService carListingPolicyService;
    private final CarMarketInsightService carMarketInsightService;
    private final CarGalleryMediaService carGalleryMediaService;
    private final CarExhaustionRowProcessor carExhaustionRowProcessor;

    @Autowired
    public CarServiceImpl(
            final CarDao carDao,
            final UserService userService,
            final UserReadinessService userReadinessService,
            final UserLocaleService userLocaleService,
            @Lazy final CarAvailabilityService carAvailabilityService,
            final EmailService emailService,
            final StoredFileService storedFileService,
            @Lazy final CarSearchService carSearchService,
            @Lazy final AdminService adminService,
            final CarListingPolicyService carListingPolicyService,
            final CarMarketInsightService carMarketInsightService,
            final CarGalleryMediaService carGalleryMediaService,
            final CarExhaustionRowProcessor carExhaustionRowProcessor) {
        this.carDao = carDao;
        this.userService = userService;
        this.userReadinessService = userReadinessService;
        this.userLocaleService = userLocaleService;
        this.carAvailabilityService = carAvailabilityService;
        this.emailService = emailService;
        this.storedFileService = storedFileService;
        this.carSearchService = carSearchService;
        this.adminService = adminService;
        this.carListingPolicyService = carListingPolicyService;
        this.carMarketInsightService = carMarketInsightService;
        this.carGalleryMediaService = carGalleryMediaService;
        this.carExhaustionRowProcessor = carExhaustionRowProcessor;
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
        if (carDao.existsByOwnerAndPlate(ownerId, plate)) {
            throw new DuplicatePlateException(plate);
        }
        try {
            return carDao.createCar(ownerId, plate, carModelId, year, powertrain, transmission);
        } catch (final DataIntegrityViolationException ex) {
            throw new DuplicatePlateException(plate);
        }
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
        carListingPolicyService.requireOwnerNotBlocked(ownerId);
        if (carDao.existsByOwnerAndPlate(ownerId, plate)) {
            throw new DuplicatePlateException(plate);
        }
        if (carGalleryMediaService.countNonEmptyGalleryUploads(galleryMedia) < 1
                || !carGalleryMediaService.hasNonEmptyImageUpload(galleryMedia)) {
            throw new CarValidationException(MessageKeys.CAR_GALLERY_PICTURES_REQUIRED);
        }
        final Car car;
        try {
            car = carDao.createCar(ownerId, plate, carModelId, year, powertrain, transmission);
        } catch (final DataIntegrityViolationException ex) {
            // Concurrent publish with the same owner+plate (PG UNIQUE / HSQL unique index).
            throw new DuplicatePlateException(plate);
        }
        if (description != null && !description.isBlank()) {
            car.setDescription(description.strip());
        }
        carGalleryMediaService.attachGalleryMedia(ownerId, car.getId(), galleryMedia, 1);
        if (insuranceData != null && insuranceData.length > 0) {
            final StoredFile stored = storedFileService.create(
                    ownerId,
                    insuranceFilename != null ? insuranceFilename : "insurance",
                    insuranceContentType != null ? insuranceContentType : "application/octet-stream",
                    insuranceData);
            // Dirty-check the managed entity — avoid PESSIMISTIC_WRITE find on a row that may
            // not be flushed yet (StaleObjectStateException right after createCar).
            car.setInsuranceFile(stored);
            car.setUpdatedAt(OffsetDateTime.now());
        } else {
            car.setStatus(Car.Status.LACK_DOC);
            car.setUpdatedAt(OffsetDateTime.now());
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
    @Transactional
    public void lockForReservationWrite(final long carId) {
        carDao.lockForReservationWrite(carId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getOwnerCarCards(final OwnerCarSearchCriteria criteria) {
        return carDao.getOwnerCarCards(criteria);
    }

    @Override
    @Transactional
    public Car.Status toggleCarStatus(final long ownerId, final long carId) {
        carDao.lockForReservationWrite(carId);
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
            if (ownerRow.isEmpty() || !userReadinessService.hasValidCbu(ownerRow.get())) {
                throw new CarValidationException(
                        MessageKeys.CAR_ACTIVATE_CBU_REQUIRED, CbuRules.REQUIRED_DIGIT_LENGTH);
            }
            if (ownerRow.get().getIdentityFileId().isEmpty()) {
                throw new CarValidationException(MessageKeys.CAR_ACTIVATE_IDENTITY_REQUIRED);
            }
            // Blocked owners (e.g. unpaid refund proofs) cannot resume their own listings.
            if (ownerRow.get().isBlocked()) {
                throw new CarValidationException(MessageKeys.CAR_ACTIVATE_OWNER_BLOCKED);
            }
            // Defense-in-depth: PAUSED may still lack docs (clearing insurance/CBU/identity only demotes
            // ACTIVE → LACK_DOC so owner intent is preserved). Re-check insurance before promoting.
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
        carDao.lockForReservationWrite(carId);
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            return false;
        }
        carDao.setCarStatus(carId, Car.Status.DEACTIVATED);
        return true;
    }

    @Override
    @Transactional
    public void applyStatusTransition(
            final long carId,
            final Car.Status target,
            final long actingUserId) {
        carDao.lockForReservationWrite(carId);
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        assertStatusPatchAuthorized(car, target, actingUserId);
        switch (target) {
            case ACTIVE -> {
                if (car.getStatus() == Car.Status.ADMIN_PAUSED) {
                    adminService.adminResumeCar(carId, actingUserId);
                } else {
                    if (car.getStatus() != Car.Status.PAUSED) {
                        throw new CarStatusTransitionConflictException(carId);
                    }
                    toggleCarStatus(actingUserId, carId);
                }
            }
            case PAUSED -> {
                if (car.getStatus() != Car.Status.ACTIVE) {
                    throw new CarStatusTransitionConflictException(carId);
                }
                toggleCarStatus(actingUserId, carId);
            }
            case ADMIN_PAUSED -> adminService.adminPauseCar(carId, actingUserId);
            case DEACTIVATED -> throw new CarStatusTransitionConflictException(carId);
            default -> throw new CarStatusTransitionConflictException(carId);
        }
    }

    private void assertStatusPatchAuthorized(
            final Car car,
            final Car.Status target,
            final long actingUserId) {
        final boolean actingUserIsAdmin = userService.findRolesForUser(actingUserId).contains(UserRole.ADMIN);
        switch (target) {
            case ACTIVE -> {
                if (car.getStatus() == Car.Status.ADMIN_PAUSED) {
                    if (!actingUserIsAdmin) {
                        throw new AdminPromoterNotAdminException();
                    }
                } else if (car.getOwnerId() != actingUserId) {
                    throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
                }
            }
            case PAUSED, DEACTIVATED -> {
                if (car.getOwnerId() != actingUserId) {
                    throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
                }
            }
            case ADMIN_PAUSED -> {
                if (!actingUserIsAdmin) {
                    throw new AdminPromoterNotAdminException();
                }
            }
            default -> throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarCard> findSimilarCarCards(final long carId, final int limit, final User viewer) {
        if (limit <= 0) {
            return List.of();
        }
        final LocalDate minBookableWallDate = carSearchService.publicBrowseMinBookableWallDate();
        return BookableBrowseSupport.retainBookableCards(
                carDao.findSimilarCarCards(carId, limit, minBookableWallDate, null),
                carAvailabilityService);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getCheapestCarCards(final int page, final int pageSize) {
        // Home browse intentionally does not exclude the viewer's own cars: owners want to see
        // how their own listings render alongside the rest of the catalog.
        // Bookable eligibility is the DAO SQL predicate (offered end_date >= browseWallDate):
        // one COUNT + one ID page, memory O(pageSize) — no JVM catalogue materialisation.
        final LocalDate minBookableWallDate = carSearchService.publicBrowseMinBookableWallDate();
        return carDao.getCheapestCarCards(page, pageSize, minBookableWallDate, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> getMostRecentCarCards(final int page, final int pageSize) {
        final LocalDate minBookableWallDate = carSearchService.publicBrowseMinBookableWallDate();
        return carDao.getMostRecentCarCards(page, pageSize, minBookableWallDate, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CarCard> searchCarCards(final CarSearchCriteria criteria) {
        // When browseWallDate is set (no explicit rider range), CarJpaDao applies the same
        // EXISTS/JOIN bookable predicate on COUNT and the ID page — totals match the page.
        return carDao.searchCarCards(criteria);
    }

    /**
     * Deliberately NOT {@code @Transactional}: delegates to criteria builder (no persistence here).
     */
    @Override
    public CarSearchCriteria buildSearchCriteria(final CarSearchRequest request) {
        return carSearchService.buildSearchCriteria(request);
    }

    /**
     * Deliberately NOT {@code @Transactional}: delegates to criteria builder (no persistence here).
     */
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

    /**
     * Deliberately NOT {@code @Transactional}: delegates to criteria builder (no persistence here).
     */
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

    /**
     * Deliberately NOT {@code @Transactional}: pure in-memory policy (no persistence).
     */
    @Override
    public List<Car.Status> resolveOwnerListingStatuses(
            final List<Car.Status> requestedStatuses, final boolean viewerIsSelfOrAdmin) {
        return carListingPolicyService.resolveOwnerListingStatuses(requestedStatuses, viewerIsSelfOrAdmin);
    }

    /**
     * Deliberately NOT {@code @Transactional}: each pause commits in
     * {@link CarExhaustionRowProcessor} ({@code REQUIRES_NEW}); an outer TX would hold the
     * connection for the whole catalog scan and roll back all pauses on a single failure.
     */
    @Override
    public void refreshExhaustedCarsToPaused() {
        final int batchSize = 200;
        Long afterId = null;
        int paused = 0;
        int scanned = 0;
        while (true) {
            final List<Long> activeCarIds =
                    carDao.findCarIdsByStatusAfter(Car.Status.ACTIVE, afterId, batchSize);
            if (activeCarIds.isEmpty()) {
                break;
            }
            scanned += activeCarIds.size();
            afterId = activeCarIds.get(activeCarIds.size() - 1);
            // Batch the bookable-day calculation for this page: the calendar service turns the N+1
            // (one availability + one blocking-reservation query per car) into 2 queries total.
            final Map<Long, List<AvailabilityPeriod>> bookableByCarId =
                    carAvailabilityService.getBookableWallAvailabilityPeriodsByCars(activeCarIds);
            for (final Long carId : activeCarIds) {
                final List<AvailabilityPeriod> bookable = bookableByCarId.getOrDefault(carId, List.of());
                if (bookable.isEmpty() && carExhaustionRowProcessor.pauseIfStillActive(carId)) {
                    paused++;
                }
            }
            if (activeCarIds.size() < batchSize) {
                break;
            }
        }
        if (scanned == 0) {
            LOGGER.atInfo().log("Car exhaustion sweep: no active cars");
            return;
        }
        LOGGER.atInfo()
                .addArgument(paused)
                .addArgument(scanned)
                .log("Car exhaustion sweep: paused {} of {} active cars");
    }

    @Override
    @Transactional
    public List<Long> pauseCarsForMissingCbu(final long ownerId) {
        return pauseActiveCarsToLackDoc(ownerId, (owner, car, locale) ->
                emailService.sendListingPausedDueToMissingCbu(CarPausedMissingCbuOwnerEmailPayload.builder()
                        .messageLocale(locale)
                        .ownerEmail(owner.getEmail())
                        .ownerFullName(owner.getForename() + " " + owner.getSurname())
                        .vehicleLabel(carLabel(car))
                        .carId(car.getId())
                        .build()));
    }

    @Override
    @Transactional
    public void resumeCarsForRestoredCbu(final long ownerId) {
        resumeEligibleLackDocCars(ownerId);
    }

    @Override
    @Transactional
    public List<Long> pauseCarsForMissingIdentity(final long ownerId) {
        return pauseActiveCarsToLackDoc(ownerId, (owner, car, locale) ->
                emailService.sendListingPausedDueToMissingIdentity(CarPausedMissingIdentityOwnerEmailPayload.builder()
                        .messageLocale(locale)
                        .ownerEmail(owner.getEmail())
                        .ownerFullName(owner.getForename() + " " + owner.getSurname())
                        .vehicleLabel(carLabel(car))
                        .carId(car.getId())
                        .build()));
    }

    @Override
    @Transactional
    public void resumeCarsForRestoredIdentity(final long ownerId) {
        resumeEligibleLackDocCars(ownerId);
    }

    @FunctionalInterface
    private interface ListingPausedMailSender {
        void send(User owner, Car car, Locale locale);
    }

    /**
     * Demotes only {@link Car.Status#ACTIVE} listings to {@link Car.Status#LACK_DOC}.
     * PAUSED / ADMIN_PAUSED / DEACTIVATED stay put so restoring docs cannot reopen a listing
     * the owner (or admin) intentionally left off the market.
     */
    private List<Long> pauseActiveCarsToLackDoc(final long ownerId, final ListingPausedMailSender mailSender) {
        final List<Car> active = carDao.findCarsByOwnerAndStatuses(
                ownerId, List.of(Car.Status.ACTIVE));
        if (active.isEmpty()) {
            return List.of();
        }
        final User owner = userService.getUserById(ownerId).orElse(null);
        if (owner == null) {
            return List.of();
        }
        final String ownerEmail = owner.getEmail();
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return List.of();
        }
        final Locale ownerMailLocale = userLocaleService.resolveMailLocaleFor(owner);
        final List<Long> pausedCarIds = new ArrayList<>();
        for (final Car car : active) {
            carDao.lockForReservationWrite(car.getId());
            if (!carDao.updateCarStatusIfCurrent(car.getId(), Car.Status.LACK_DOC, Car.Status.ACTIVE)) {
                continue;
            }
            pausedCarIds.add(car.getId());
            mailSender.send(owner, car, ownerMailLocale);
        }
        return pausedCarIds;
    }

    private void resumeEligibleLackDocCars(final long ownerId) {
        final List<Car> toResume = carDao.findCarsByOwnerAndStatuses(ownerId, List.of(Car.Status.LACK_DOC));
        final User owner = userService.getUserById(ownerId).orElse(null);
        if (owner == null || !userReadinessService.meetsPublishingPrerequisites(owner)) {
            return;
        }
        for (final Car car : toResume) {
            // Only re-activate when insurance is also present; missing insurance alone keeps LACK_DOC.
            if (car.getInsuranceFileId().isPresent()) {
                carDao.lockForReservationWrite(car.getId());
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
        carListingPolicyService.requireOwnerNotBlocked(ownerId);
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        carListingPolicyService.assertInsurancePayloadValid(contentType, data);
        final Car car = carOpt.get();
        final Long previousInsuranceId = car.getInsuranceFileId().orElse(null);
        final StoredFile stored = storedFileService.create(
                ownerId,
                originalFilename != null ? originalFilename : "insurance",
                contentType,
                data);
        carDao.lockForReservationWrite(carId);
        carDao.updateInsuranceDocument(carId, stored.getId());
        if (previousInsuranceId != null && previousInsuranceId > 0 && !previousInsuranceId.equals(stored.getId())) {
            storedFileService.deleteById(previousInsuranceId);
        }
        final Car locked = carDao.getCarById(carId).orElse(car);
        if (locked.getStatus() == Car.Status.LACK_DOC) {
            final User owner = userService.getUserById(ownerId).orElse(null);
            if (owner != null && userReadinessService.meetsPublishingPrerequisites(owner)) {
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
        final Car car = carOpt.get();
        final Long previousInsuranceId = car.getInsuranceFileId().orElse(null);
        carDao.lockForReservationWrite(carId);
        carDao.clearInsuranceDocument(carId);
        if (previousInsuranceId != null && previousInsuranceId > 0) {
            storedFileService.deleteById(previousInsuranceId);
        }
        final Car locked = carDao.getCarById(carId).orElse(car);
        // Only ACTIVE → LACK_DOC. PAUSED / ADMIN_PAUSED / DEACTIVATED keep their status;
        // toggle / admin release still re-check docs before returning to the market.
        if (locked.getStatus() == Car.Status.ACTIVE) {
            carDao.setCarStatus(carId, Car.Status.LACK_DOC);
        }
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
    public void updateDescription(final long ownerId, final long carId, final String description) {
        final Optional<Car> carOpt = carDao.getCarById(carId);
        if (carOpt.isEmpty() || carOpt.get().getOwnerId() != ownerId) {
            throw new CarValidationException(MessageKeys.CAR_NOT_FOUND);
        }
        final Car car = carOpt.get();
        if (description != null && !description.isBlank()) {
            car.setDescription(description.strip());
        } else {
            car.setDescription(null);
        }
    }

    @Override
    @Transactional
    public void updateRatingAvg(final long carId, final BigDecimal average) {
        carDao.updateRatingAvg(carId, average);
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
        // Serialize this status flip against concurrent status transitions and reservation writes on
        // the same car row (same PESSIMISTIC_WRITE lock as lockForReservationWrite). Reentrant when
        // reached through applyStatusTransition, which already holds the lock.
        carDao.lockForReservationWrite(carId);
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        if (car.getStatus() == Car.Status.DEACTIVATED) {
            throw new CarValidationException(MessageKeys.CAR_INVALID_STATUS_TRANSITION);
        }
        car.setStatus(Car.Status.ADMIN_PAUSED);
    }

    @Override
    @Transactional
    public void releaseAdminCarPause(final long carId) {
        // Serialize this status flip against concurrent status transitions and reservation writes on
        // the same car row (same PESSIMISTIC_WRITE lock as lockForReservationWrite). Reentrant when
        // reached through applyStatusTransition, which already holds the lock.
        carDao.lockForReservationWrite(carId);
        final Car car = carDao.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        if (car.getStatus() != Car.Status.ADMIN_PAUSED) {
            throw new CarNotAdminPausedException(carId);
        }
        // markCarAsAdminPaused accepts every non-DEACTIVATED status (including LACK_DOC), so we cannot
        // assume the car had valid documentation when it was paused. The Car.Status javadoc reserves
        // ACTIVE for listings with all required documents, so re-check insurance and publishing
        // prerequisites (CBU + identity) before promoting; otherwise drop back to LACK_DOC and let
        // the regular re-activation path bring the car back online once the owner completes them.
        final boolean hasInsurance = car.getInsuranceFileId().isPresent();
        final boolean publishingReady = userService.getUserById(car.getOwnerId())
                .map(userReadinessService::meetsPublishingPrerequisites)
                .orElse(false);
        if (hasInsurance && publishingReady) {
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
        return carMarketInsightService.getPriceMarketInsightForCar(car, excludeCarId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ConsumerCarCardMarketContext> resolveConsumerPriceMarketContexts(
            final List<CarCard> cards) {
        return carMarketInsightService.resolveConsumerPriceMarketContexts(cards);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CarPriceMarketInsight> findActiveDayPriceMarketInsightByBrandAndModel(
            final String brand, final String model, final Long excludeCarId) {
        return carDao.findActiveDayPriceMarketInsightByBrandAndModel(brand, model, excludeCarId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModelPriceSample> findActiveDayPricesForBrandModelPairs(
            final List<String> brands, final List<String> models) {
        return carDao.findActiveDayPricesForBrandModelPairs(brands, models);
    }
}