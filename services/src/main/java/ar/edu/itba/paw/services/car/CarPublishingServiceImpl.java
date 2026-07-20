package ar.edu.itba.paw.services.car;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.dto.PublishCarOutcome;
import ar.edu.itba.paw.dto.PublishCarRequest;
import ar.edu.itba.paw.exception.car.CarPublishPrerequisitesMissingException;
import ar.edu.itba.paw.exception.car.CarValidationException;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.user.User;

import ar.edu.itba.paw.services.user.UserReadinessService;
import ar.edu.itba.paw.services.user.AdminService;
import ar.edu.itba.paw.services.user.UserService;
/**
 * Orchestrates brand/model resolution and the admin-vs-owner publishing branch. The decision
 * of "publish now / publish pending / admin auto-validates" used to live in
 * {@code PublishCarFormController.publish} — this service owns it and exposes a single typed
 * {@link PublishCarOutcome} the controller can switch on.
 */
@Service
public class CarPublishingServiceImpl implements CarPublishingService {

    private final CarService carService;
    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final UserService userService;
    private final UserReadinessService userReadinessService;
    private final AdminService adminService;

    @Autowired
    public CarPublishingServiceImpl(
            final CarService carService,
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final UserService userService,
            final UserReadinessService userReadinessService,
            final AdminService adminService) {
        this.carService = carService;
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.userService = userService;
        this.userReadinessService = userReadinessService;
        this.adminService = adminService;
    }

    @Override
    @Transactional
    public PublishCarOutcome publishCar(final long ownerId, final PublishCarRequest request) {
        final User owner = userService.getUserById(ownerId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        if (!userReadinessService.meetsPublishingPrerequisites(owner)) {
            throw new CarPublishPrerequisitesMissingException();
        }

        // Resolve brand/model strings (already non-blank-validated upstream) to a catalog row.
        // findOrCreateUnvalidated() returns a row marked validated=false when the value is new.
        final CarBrand resolvedBrand = carBrandService.findOrCreateUnvalidated(request.getBrand())
                .orElseThrow(() -> new CarValidationException(MessageKeys.CATALOG_BRAND_NOT_FOUND, request.getBrand()));
        final CarModel resolvedModel = carModelService
                .findOrCreateUnvalidated(resolvedBrand.getId(), request.getModel(), request.getType())
                .orElseThrow(() -> new CarValidationException(MessageKeys.CATALOG_MODEL_NOT_FOUND, request.getModel()));

        final boolean newCatalogEntry = !resolvedBrand.isValidated() || !resolvedModel.isValidated();

        // Admins skip the pending-validation flow: they're trusted to add catalog entries directly.
        if (newCatalogEntry && owner.isAdmin()) {
            adminService.validateCatalogEntry(resolvedModel.getId());
        }

        final Car car = carService.publishCar(
                ownerId,
                request.getPlate(),
                resolvedModel.getId(),
                request.getYear(),
                request.getPowertrain(),
                request.getTransmission(),
                request.getDescription(),
                request.getGalleryUploads(),
                request.getInsuranceFilename().orElse(null),
                request.getInsuranceContentType().orElse(null),
                request.getInsuranceBytes());

        final Integer minimumRentalDays = request.getMinimumRentalDays();
        if (minimumRentalDays != null && minimumRentalDays > 0) {
            carService.updateMinimumRentalDays(car.getId(), minimumRentalDays);
        }

        // Non-admins introducing a new catalog entry get the pending-validation outcome so
        // the controller can redirect to a different view explaining the wait.
        if (newCatalogEntry && !owner.isAdmin()) {
            return PublishCarOutcome.pending(
                    car,
                    resolvedBrand.isValidated() ? null : resolvedBrand.getName(),
                    resolvedModel.isValidated() ? null : resolvedModel.getName());
        }
        return PublishCarOutcome.published(car, newCatalogEntry);
    }
}
