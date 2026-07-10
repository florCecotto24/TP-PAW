package ar.edu.itba.paw.services.user;


import java.util.ArrayList;
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
import ar.edu.itba.paw.exception.admin.AdminCannotPauseAdminCarException;
import ar.edu.itba.paw.exception.car.CarBrandNotFoundException;
import ar.edu.itba.paw.exception.car.CarModelNotFoundException;
import ar.edu.itba.paw.exception.car.CarNotFoundException;
import ar.edu.itba.paw.exception.MessageKeys;
import ar.edu.itba.paw.exception.user.UserNotFoundException;
import ar.edu.itba.paw.models.domain.car.Car;
import ar.edu.itba.paw.models.domain.car.CarBrand;
import ar.edu.itba.paw.models.domain.car.CarModel;
import ar.edu.itba.paw.models.domain.reservation.Reservation;
import ar.edu.itba.paw.models.domain.reservation.ReservationMessage;
import ar.edu.itba.paw.models.domain.user.User;
import ar.edu.itba.paw.models.dto.Page;
import ar.edu.itba.paw.models.dto.reservation.AdminReservationChatPageModel;
import ar.edu.itba.paw.models.dto.reservation.ReservationCard;
import ar.edu.itba.paw.models.security.UserRole;
import ar.edu.itba.paw.models.email.admin.AdminInvitationEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarPausedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarRejectedByAdminOwnerEmailPayload;
import ar.edu.itba.paw.models.email.listing.CarValidatedByAdminOwnerEmailPayload;

import ar.edu.itba.paw.services.car.CarBrandService;
import ar.edu.itba.paw.services.car.CarModelService;
import ar.edu.itba.paw.services.car.CarService;
import ar.edu.itba.paw.services.email.EmailService;
import ar.edu.itba.paw.services.reservation.ReservationMessageService;
import ar.edu.itba.paw.services.reservation.ReservationService;
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
    public void demoteUserFromAdmin(final long targetUserId, final long assignedByUserId) {
        userService.demoteFromAdmin(targetUserId, assignedByUserId);
    }

    @Override
    public User createAdminUser(
            final String email,
            final String forename,
            final String surname,
            final String temporaryPassword,
            final long assignedByUserId,
            final Locale locale) {
        final User newUser = userService.createAdminUserWithEncodedPassword(
                email, forename, surname, passwordEncoder.encode(temporaryPassword), assignedByUserId);
        emailService.sendAdminInvitation(
                AdminInvitationEmailPayload.builder()
                        .messageLocale(locale)
                        .recipientEmail(email)
                        .recipientFullName(forename + " " + surname)
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
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
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
    @Transactional
    public void deleteUser(final long targetUserId, final long actingAdminId) {
        if (targetUserId == actingAdminId) {
            throw new AdminCannotBlockSelfException();
        }
        final User target = userService.getUserById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(MessageKeys.USER_ACCOUNT_NOT_FOUND));
        target.getRoleAssignedBy().ifPresent(grantorId -> {
            if (grantorId == actingAdminId) {
                throw new AdminCannotBlockGrantorException();
            }
        });
        userService.deleteUser(targetUserId);
    }

    @Override
    public void adminPauseCar(final long carId, final long actingAdminId, final Locale locale) {
        final Car car = carService.getCarById(carId)
                .orElseThrow(() -> new CarNotFoundException(carId));
        if (car.getOwner().isAdmin()) {
            throw new AdminCannotPauseAdminCarException(carId);
        }
        carService.markCarAsAdminPaused(carId);
        final List<Reservation> blocking = reservationService.findBlockingReservationsByCarId(carId);
        for (final Reservation r : blocking) {
            if (r.getStatus() == Reservation.Status.PENDING || r.getStatus() == Reservation.Status.ACCEPTED) {
                reservationService.cancelReservation(r.getId());
            }
        }
        LOGGER.atInfo().addArgument(actingAdminId).addArgument(carId).log("Admin {} paused car {}");
        final User owner = car.getOwner();
        final String vehicleLabel = (car.getBrand() != null ? car.getBrand() : "")
                + (car.getBrand() != null && car.getModel() != null ? " " : "")
                + (car.getModel() != null ? car.getModel() : "");
        emailService.sendCarPausedByAdmin(
                CarPausedByAdminOwnerEmailPayload.builder()
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
        LOGGER.atInfo().addArgument(actingAdminId).addArgument(carId).log("Admin {} resumed car {}");
    }

    @Override
    public void validateCarBrand(final long brandId) {
        if (carBrandService.findById(brandId).isEmpty()) {
            throw new CarBrandNotFoundException(brandId);
        }
        carBrandService.markAsValidated(brandId);
    }

    @Override
    public void rejectCarBrand(final long brandId) {
        carBrandService.findById(brandId).ifPresent(brand -> {
            LOGGER.atWarn()
                    .addArgument(brandId)
                    .addArgument(brand.getName())
                    .log("Admin rejecting brand id={} name='{}'; cars referencing this brand/model become orphaned");
            carBrandService.deleteById(brandId);
        });
    }

    @Override
    public void validateCarModel(final long modelId) {
        if (carModelService.findById(modelId).isEmpty()) {
            throw new CarModelNotFoundException(modelId);
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
                .orElseThrow(() -> new CarModelNotFoundException(modelId));
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
            emailService.sendCarValidatedByAdmin(
                    CarValidatedByAdminOwnerEmailPayload.builder()
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
                .orElseThrow(() -> new CarModelNotFoundException(modelId));
        final CarBrand brand = model.getBrand();
        final boolean brandIsPending = !brand.isValidated();
        final boolean isLastModelForBrand = carModelService.countByBrandId(brand.getId()) == 1;
        final boolean brandWillBeRejected = brandIsPending && isLastModelForBrand;
        final String brandName = brand.getName();
        final String modelName = model.getName();
        final List<Car> affectedCars = carService.findCarsByModelId(modelId);
        final List<CarRejectedByAdminOwnerEmailPayload> notifications = new ArrayList<>(affectedCars.size());
        for (final Car car : affectedCars) {
            final User owner = car.getOwner();
            final String vehicleLabel = brandName + " " + modelName;
            notifications.add(
                    CarRejectedByAdminOwnerEmailPayload.builder()
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
        for (final CarRejectedByAdminOwnerEmailPayload payload : notifications) {
            emailService.sendCarRejectedByAdmin(payload);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsers(
            final int page,
            final int pageSize,
            final Boolean blocked,
            final String role,
            final String query) {
        final UserRole roleFilter = parseAdminRoleFilter(role);
        return userService.findUsersPaginated(page, pageSize, blocked, roleFilter, query);
    }

    private static UserRole parseAdminRoleFilter(final String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        if ("admin".equalsIgnoreCase(role.trim())) {
            return UserRole.ADMIN;
        }
        if ("user".equalsIgnoreCase(role.trim())) {
            return UserRole.USER;
        }
        throw new IllegalArgumentException("Invalid role filter: " + role);
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
    public Page<Car> listCars(final int page, final int pageSize) {
        return carService.findAllCarsPaginated(page, pageSize);
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
    public List<ReservationMessage> getChatMessagesAfter(
            final long reservationId, final long afterMessageId, final int limit) {
        return reservationMessageService.findMessagesAfter(reservationId, afterMessageId, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public long countReservationMessages(final long reservationId) {
        return reservationMessageService.countMessages(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminReservationChatPageModel> loadReservationChatPage(
            final long reservationId, final int page, final int pageSize) {
        final int safePage = Math.max(0, page);
        final int safePageSize = Math.max(1, pageSize);
        return reservationService.getReservationById(reservationId)
                .map(reservation -> new AdminReservationChatPageModel(
                        reservation,
                        reservationMessageService.getAdminChatMessages(
                                reservationId, safePage * safePageSize, safePageSize),
                        reservationMessageService.countMessages(reservationId),
                        safePage,
                        safePageSize));
    }

}
