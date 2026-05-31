package ar.edu.itba.paw.services;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.itba.paw.exception.admin.AdminCannotBlockGrantorException;
import ar.edu.itba.paw.exception.admin.AdminCannotBlockSelfException;
import ar.edu.itba.paw.models.domain.Car;
import ar.edu.itba.paw.models.domain.CarBrand;
import ar.edu.itba.paw.models.domain.CarModel;
import ar.edu.itba.paw.models.domain.Reservation;
import ar.edu.itba.paw.models.domain.ReservationMessage;
import ar.edu.itba.paw.models.domain.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.email.ListingPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.ListingRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.ListingValidatedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.MigratedUserPasswordEmailPayload;

/**
 * Admin operations: user management, car moderation, catalog validation, and reservation inspection.
 *
 * <p>This service is a pure orchestrator: it never talks to any DAO directly (project rule: a service may
 * only call its own DAO). All persistence access is delegated to the corresponding peer services
 * ({@link UserService}, {@link CarService}, {@link CarBrandService}, {@link CarModelService},
 * {@link ReservationService}, {@link ReservationMessageService}).</p>
 */
@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserService userService;
    private final CarService carService;
    private final CarBrandService carBrandService;
    private final CarModelService carModelService;
    private final ReservationService reservationService;
    private final ReservationMessageService reservationMessageService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminServiceImpl(
            final UserService userService,
            final CarService carService,
            final CarBrandService carBrandService,
            final CarModelService carModelService,
            final ReservationService reservationService,
            final ReservationMessageService reservationMessageService,
            final EmailService emailService,
            final PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.carService = carService;
        this.carBrandService = carBrandService;
        this.carModelService = carModelService;
        this.reservationService = reservationService;
        this.reservationMessageService = reservationMessageService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void promoteUserToAdmin(final long targetUserId, final long assignedByUserId) {
        userService.promoteToAdmin(targetUserId, assignedByUserId);
    }

    @Override
    public User createAdminUser(
            final String email,
            final String forename,
            final String surname,
            final String temporaryPassword,
            final long assignedByUserId,
            final Locale locale) {
        final User newUser = userService.createUserWithEncodedPassword(
                email, forename, surname, passwordEncoder.encode(temporaryPassword));
        userService.promoteToAdmin(newUser.getId(), assignedByUserId);
        userService.markEmailVerified(newUser.getId());
        emailService.sendMigratedUserPassword(
                MigratedUserPasswordEmailPayload.builder()
                        .messageLocale(locale)
                        .recipientEmail(email)
                        .plainPassword(temporaryPassword)
                        .build());
        return newUser;
    }

    @Override
    public void blockUser(final long targetUserId, final long actingAdminId) {
        if (actingAdminId == targetUserId) {
            throw new AdminCannotBlockSelfException();
        }
        final User target = userService.getUserById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUserId));
        target.getRoleAssignedBy().ifPresent(grantorId -> {
            if (grantorId == actingAdminId) {
                throw new AdminCannotBlockGrantorException();
            }
        });
        userService.blockUser(targetUserId);
    }

    @Override
    public void unblockUser(final long targetUserId) {
        userService.unblockUser(targetUserId);
    }

    @Override
    public void adminPauseCar(final long carId, final long actingAdminId, final Locale locale) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new IllegalArgumentException("Car not found: " + carId));
        if (car.getOwner().isAdmin()) {
            throw new IllegalArgumentException("Cannot admin-pause a car owned by another admin: " + carId);
        }
        carService.markCarAsAdminPaused(carId);
        final List<Reservation> blocking = reservationService.findBlockingReservationsByCarId(carId);
        for (final Reservation r : blocking) {
            if (r.getStatus() == Reservation.Status.PENDING || r.getStatus() == Reservation.Status.ACCEPTED) {
                reservationService.cancelReservation(r.getId());
            }
        }
        LOGGER.info("Admin {} paused car {}", actingAdminId, carId);
        final User owner = car.getOwner();
        final String vehicleLabel = (car.getBrand() != null ? car.getBrand() : "")
                + (car.getBrand() != null && car.getModel() != null ? " " : "")
                + (car.getModel() != null ? car.getModel() : "");
        emailService.sendListingPausedByAdmin(
                ListingPausedByAdminOwnerEmailPayload.builder()
                        .messageLocale(locale)
                        .ownerEmail(owner.getEmail())
                        .ownerFullName(owner.getForename() + " " + owner.getSurname())
                        .vehicleLabel(vehicleLabel)
                        .carId(carId)
                        .build());
    }

    @Override
    public void adminResumeCar(final long carId, final long actingAdminId) {
        carService.releaseAdminCarPause(carId);
        LOGGER.info("Admin {} resumed car {}", actingAdminId, carId);
    }

    @Override
    public void validateCarBrand(final long brandId) {
        if (carBrandService.findById(brandId).isEmpty()) {
            throw new IllegalArgumentException("Brand not found: " + brandId);
        }
        carBrandService.markAsValidated(brandId);
    }

    @Override
    public void rejectCarBrand(final long brandId) {
        carBrandService.findById(brandId).ifPresent(brand -> {
            LOGGER.warn("Admin rejecting brand id={} name='{}'; cars referencing this brand/model become orphaned",
                    brandId, brand.getName());
            carBrandService.deleteById(brandId);
        });
    }

    @Override
    public void validateCarModel(final long modelId) {
        if (carModelService.findById(modelId).isEmpty()) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }
        carModelService.markAsValidated(modelId);
    }

    @Override
    public void rejectCarModel(final long modelId) {
        carModelService.deleteById(modelId);
    }

    @Override
    public void validateCatalogEntry(final long modelId, final Locale locale) {
        final CarModel model = carModelService.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
        final CarBrand brand = model.getBrand();
        final boolean brandWillBeValidated = !brand.isValidated();
        final String brandName = brand.getName();
        final String modelName = model.getName();
        final List<Car> affectedCars = carService.findCarsByModelId(modelId);
        if (brandWillBeValidated) {
            carBrandService.markAsValidated(brand.getId());
        }
        carModelService.markAsValidated(modelId);
        for (final Car car : affectedCars) {
            final User owner = car.getOwner();
            final String vehicleLabel = brandName + " " + modelName;
            emailService.sendListingValidatedByAdmin(
                    ListingValidatedByAdminOwnerEmailPayload.builder()
                            .messageLocale(locale != null ? locale : Locale.ENGLISH)
                            .ownerEmail(owner.getEmail())
                            .ownerFullName(owner.getForename() + " " + owner.getSurname())
                            .vehicleLabel(vehicleLabel)
                            .carId(car.getId())
                            .brandName(brandName)
                            .modelName(modelName)
                            .brandValidated(brandWillBeValidated)
                            .build());
        }
    }

    @Override
    public void rejectCatalogEntry(final long modelId, final Locale locale) {
        final CarModel model = carModelService.findById(modelId)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
        final CarBrand brand = model.getBrand();
        final boolean brandIsPending = !brand.isValidated();
        final boolean isLastModelForBrand = carModelService.countByBrandId(brand.getId()) == 1;
        final boolean brandWillBeRejected = brandIsPending && isLastModelForBrand;
        final String brandName = brand.getName();
        final String modelName = model.getName();
        final List<Car> affectedCars = carService.findCarsByModelId(modelId);
        final List<ListingRejectedByAdminOwnerEmailPayload> notifications = new java.util.ArrayList<>(affectedCars.size());
        for (final Car car : affectedCars) {
            final User owner = car.getOwner();
            final String vehicleLabel = brandName + " " + modelName;
            notifications.add(
                    ListingRejectedByAdminOwnerEmailPayload.builder()
                            .messageLocale(locale != null ? locale : Locale.ENGLISH)
                            .ownerEmail(owner.getEmail())
                            .ownerFullName(owner.getForename() + " " + owner.getSurname())
                            .vehicleLabel(vehicleLabel)
                            .carId(car.getId())
                            .brandName(brandName)
                            .modelName(modelName)
                            .brandRejected(brandWillBeRejected)
                            .modelRejected(true)
                            .build());
            carService.clearCarModel(car.getId());
        }
        carModelService.deleteById(modelId);
        if (brandWillBeRejected) {
            carBrandService.deleteById(brand.getId());
        }
        for (final ListingRejectedByAdminOwnerEmailPayload payload : notifications) {
            emailService.sendListingRejectedByAdmin(payload);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsers(final int page, final int pageSize) {
        return userService.findAllUsersPaginated(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarBrand> findPendingBrands() {
        return carBrandService.findPendingOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModel> findPendingModels() {
        return carModelService.findPendingOrdered();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Car> findAdminPausedCars() {
        return carService.findAdminPausedCars();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationCard> listAllReservations(final int page, final int pageSize) {
        return reservationService.findAllReservationCards(page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservationById(final long reservationId) {
        return reservationService.getReservationById(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationMessage> getAdminChatMessages(final long reservationId, final int offset, final int limit) {
        return reservationMessageService.getAdminChatMessages(reservationId, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReservationMessages(final long reservationId) {
        return reservationMessageService.countMessages(reservationId);
    }

}
